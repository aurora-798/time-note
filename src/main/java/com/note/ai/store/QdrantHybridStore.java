package com.note.ai.store;

import com.note.ai.model.TemporalRange;
import com.note.config.QdrantProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.Modifier;
import io.qdrant.client.grpc.Collections.SparseVectorConfig;
import io.qdrant.client.grpc.Collections.SparseVectorParams;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorParamsMap;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Document;
import io.qdrant.client.grpc.Points.Fusion;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PrefetchQuery;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ConditionFactory.matchKeywords;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.fusion;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorFactory.vector;
import static io.qdrant.client.VectorsFactory.namedVectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantHybridStore {

    public static final String DENSE_VECTOR = "dense";
    public static final String SPARSE_VECTOR = "bm25";
    public static final String BM25_MODEL = "qdrant/bm25";
    public static final String PAYLOAD_TEXT_KEY = "text";

    private static final Duration RPC_TIMEOUT = Duration.ofSeconds(30);

    private final QdrantClient qdrantClient;
    private final QdrantProperties properties;

    @PostConstruct
    public void initCollection() {
        try {
            ensureHybridCollection();
        } catch (Exception e) {
            throw new IllegalStateException("Qdrant hybrid collection 初始化失败", e);
        }
    }

    public void ensureHybridCollection() throws ExecutionException, InterruptedException, TimeoutException {
        String collectionName = properties.getCollectionName();
        boolean exists = qdrantClient.collectionExistsAsync(collectionName)
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        if (properties.isRecreateCollection()) {
            log.info("重建 Qdrant hybrid collection: {}", collectionName);
            recreateHybridCollection(collectionName);
            return;
        }

        if (!exists) {
            log.info("创建 Qdrant hybrid collection: {}", collectionName);
            createHybridCollection(collectionName);
            return;
        }

        if (!isHybridCollection(collectionName)) {
            log.warn("collection {} 不是 hybrid 结构，正在重建", collectionName);
            recreateHybridCollection(collectionName);
        }
    }

    public void upsertSegments(List<TextSegment> segments, List<Embedding> embeddings) {
        if (segments.isEmpty()) {
            return;
        }
        if (segments.size() != embeddings.size()) {
            throw new IllegalArgumentException("segments 与 embeddings 数量不一致");
        }

        List<PointStruct> points = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            points.add(toPointStruct(segments.get(i), embeddings.get(i)));
        }

        try {
            qdrantClient.upsertAsync(properties.getCollectionName(), points)
                    .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert 失败", e);
        }
    }

    public void deleteByDiaryId(String diaryId) {
        Filter filter = Filter.newBuilder()
                .addMust(matchKeyword("diaryId", diaryId))
                .build();
        try {
            qdrantClient.deleteAsync(properties.getCollectionName(), filter)
                    .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Qdrant 删除 diaryId=" + diaryId + " 失败", e);
        }
    }

    public List<TextSegment> hybridSearch(String queryText, Embedding queryEmbedding, String userId,
                                          int limit, Optional<TemporalRange> dateRange) {
        Filter filter = buildFilter(userId, dateRange);

        Document bm25Document = Document.newBuilder()
                .setModel(BM25_MODEL)
                .setText(queryText)
                .build();

        QueryPoints request = QueryPoints.newBuilder()
                .setCollectionName(properties.getCollectionName())
                .addPrefetch(PrefetchQuery.newBuilder()
                        .setQuery(nearest(bm25Document))
                        .setUsing(SPARSE_VECTOR)
                        .setFilter(filter)
                        .setLimit(properties.getPrefetchLimit())
                        .build())
                .addPrefetch(PrefetchQuery.newBuilder()
                        .setQuery(nearest(toFloatList(queryEmbedding)))
                        .setUsing(DENSE_VECTOR)
                        .setFilter(filter)
                        .setLimit(properties.getPrefetchLimit())
                        .build())
                .setQuery(fusion(Fusion.RRF))
                .setLimit(limit)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();

        try {
            List<ScoredPoint> points = qdrantClient.queryAsync(request)
                    .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return points.stream().map(this::toTextSegment).toList();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant hybrid 检索失败", e);
        }
    }

    private void createHybridCollection(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        qdrantClient.createCollectionAsync(buildCreateCollection(collectionName))
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        createPayloadIndexes(collectionName);
    }

    private void recreateHybridCollection(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        qdrantClient.recreateCollectionAsync(buildCreateCollection(collectionName))
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        createPayloadIndexes(collectionName);
    }

    private CreateCollection buildCreateCollection(String collectionName) {
        return CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                .setVectorsConfig(VectorsConfig.newBuilder()
                        .setParamsMap(VectorParamsMap.newBuilder()
                                .putMap(DENSE_VECTOR, VectorParams.newBuilder()
                                        .setSize(properties.getEmbeddingDimensions())
                                        .setDistance(Distance.Cosine)
                                        .build())
                                .build())
                        .build())
                .setSparseVectorsConfig(SparseVectorConfig.newBuilder()
                        .putMap(SPARSE_VECTOR, SparseVectorParams.newBuilder()
                                .setModifier(Modifier.Idf)
                                .build())
                        .build())
                .build();
    }

    private boolean isHybridCollection(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        var info = qdrantClient.getCollectionInfoAsync(collectionName)
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        var params = info.getConfig().getParams();
        if (!params.hasSparseVectorsConfig()) {
            return false;
        }
        return params.getSparseVectorsConfig().getMapMap().containsKey(SPARSE_VECTOR);
    }

    private void createPayloadIndexes(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        for (String field : List.of("userId", "diaryId", "diaryDate")) {
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    field,
                    io.qdrant.client.grpc.Collections.PayloadSchemaType.Keyword,
                    null,
                    true,
                    null,
                    RPC_TIMEOUT
            ).get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private Filter buildFilter(String userId, Optional<TemporalRange> dateRange) {
        Filter.Builder builder = Filter.newBuilder()
                .addMust(matchKeyword("userId", userId));

        dateRange.ifPresent(temporal -> {
            if (temporal.isSingleDay()) {
                builder.addMust(matchKeyword("diaryDate", temporal.startDate().toString()));
            } else {
                builder.addMust(matchKeywords("diaryDate", expandDates(temporal)));
            }
        });

        return builder.build();
    }

    private List<String> expandDates(TemporalRange range) {
        List<String> dates = new ArrayList<>();
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            dates.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private PointStruct toPointStruct(TextSegment segment, Embedding embedding) {
        String text = segment.text();
        Map<String, Value> payload = new HashMap<>();
        payload.put(PAYLOAD_TEXT_KEY, value(text));
        segment.metadata().toMap().forEach((key, val) -> {
            if (val != null) {
                payload.put(key, value(String.valueOf(val)));
            }
        });

        Document bm25Document = Document.newBuilder()
                .setModel(BM25_MODEL)
                .setText(text)
                .build();

        return PointStruct.newBuilder()
                .setId(id(UUID.randomUUID()))
                .setVectors(namedVectors(Map.of(
                        DENSE_VECTOR, vector(toFloatList(embedding)),
                        SPARSE_VECTOR, vector(bm25Document)
                )))
                .putAllPayload(payload)
                .build();
    }

    private TextSegment toTextSegment(ScoredPoint point) {
        Map<String, Value> payload = point.getPayloadMap();
        String text = payload.containsKey(PAYLOAD_TEXT_KEY)
                ? payload.get(PAYLOAD_TEXT_KEY).getStringValue()
                : "";

        dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
        payload.forEach((key, val) -> {
            if (!PAYLOAD_TEXT_KEY.equals(key) && val.hasStringValue()) {
                metadata.put(key, val.getStringValue());
            }
        });
        return TextSegment.from(text, metadata);
    }

    private List<Float> toFloatList(Embedding embedding) {
        float[] vector = embedding.vector();
        List<Float> floats = new ArrayList<>(vector.length);
        for (float v : vector) {
            floats.add(v);
        }
        return floats;
    }
}
