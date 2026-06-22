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
import io.qdrant.client.grpc.Points;
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

    // Spring 容器启动后自动执行 初始化 Collection
    @PostConstruct
    public void initCollection() {
        try {
            ensureHybridCollection();
        } catch (Exception e) {
            throw new IllegalStateException("Qdrant hybrid collection 初始化失败", e);
        }
    }

    public void ensureHybridCollection() throws ExecutionException,
            InterruptedException, TimeoutException {

        // 1. collection 存在吗？-> time-note
        String collectionName = properties.getCollectionName();
        boolean exists = qdrantClient.collectionExistsAsync(collectionName)
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        // collection存在但不是 hybrid 结构 -> 重建 hybrid collection
        if (properties.isRecreateCollection()) {
            log.info("重建 Qdrant hybrid collection: {}", collectionName);
            recreateHybridCollection(collectionName);
            return;
        }

        // 不存在 -> 创建新的 hybrid collection
        if (!exists) {
            log.info("创建 Qdrant hybrid collection: {}", collectionName);
            createHybridCollection(collectionName);
            return;
        }

        // 集合不是混合检索集合
        if (!isHybridCollection(collectionName)) {
            log.warn("collection {} 不是 hybrid 结构，正在重建", collectionName);
            recreateHybridCollection(collectionName);
        }
    }

    /**
     * 批量插入向量数据
     * 批量把 LangChain4j 的 TextSegment（文本片段）+ Embedding（稠密向量）
     * 转为 Qdrant Point 写入库。
     * @param segments 文本片段集合
     * @param embeddings 向量集合
     */
    public void upsertSegments(List<TextSegment> segments, List<Embedding> embeddings) {
        // 空片段直接返回
        if (segments.isEmpty()) {
            return;
        }
        // 文本片段数量和向量数量必须一一对应，否则抛异常
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

    /**
     * 单篇日记修改 / 删除时，删除该日记所有向量片段。
     * 构造 Filter，匹配 payload 中diaryId = 指定值，Qdrant 批量删除匹配的所有 Point。
     * @param diaryId 日记 id
     */
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

    /**
     * 输入查询文本、查询稠密向量、用户 ID、日期范围，返回语义 + 关键词融合召回的文本片段。
     * queryText：用户原始提问文本（用于 BM25 稀疏检索）
     * queryEmbedding：提问文本的稠密向量（用于语义检索）
     * userId：用户隔离，只能查自己的日记（数据隔离）
     * limit：最终返回 topN 结果
     * dateRange：可选时间范围，限定只检索某段日期日记
     */
    public List<TextSegment> hybridSearch(String queryText, Embedding queryEmbedding, String userId,
                                          int limit, Optional<TemporalRange> dateRange) {
        // 1. 构建过滤条件：匹配当前用户 id 以及 时间范围处理 TemporalRange
        Filter filter = buildFilter(userId, dateRange);

        Document bm25Document = Document.newBuilder()
                .setModel(BM25_MODEL)
                .setText(queryText)
                .build();

        // 构建自定义 RRF 策略：BM25 权重为2，Dense 权重为1 k = 60
        Points.Rrf rrfConfig = Points.Rrf.newBuilder()
                .setK(60)
                .addWeights(1.7f)
                .addWeights(1.0f)
                .build();
        Points.Query query = Points.Query.newBuilder()
                .setRrf(rrfConfig)
                .build();

        QueryPoints request = QueryPoints.newBuilder()
                .setCollectionName(properties.getCollectionName())
                // 1.1 第一个 Prefetch：稀疏 BM25 检索
                .addPrefetch(PrefetchQuery.newBuilder()
                        .setQuery(nearest(bm25Document))            // 输入 bm25 稀疏向量
                        .setUsing(SPARSE_VECTOR)                    // 使用 bm25 稀疏向量
                        .setFilter(filter)                          // 携带用户、日期过滤条件
                        .setLimit(properties.getPrefetchLimit())   // 预取数量 prefetchLimit
                        .build())
                // 1.2 第二个 Prefetch：稠密语义检索
                .addPrefetch(PrefetchQuery.newBuilder()
                        .setQuery(nearest(toFloatList(queryEmbedding))) // 输入 queryEmbedding 浮点向量
                        .setUsing(DENSE_VECTOR)                     // 使用 dense 稠密向量
                        .setFilter(filter)                          // 携带用户、日期过滤条件
                        .setLimit(properties.getPrefetchLimit())    // 预取数量 prefetchLimit
                        .build())
                // 1.3 融合：使用 RRF 倒数排名融合
                // 分别从稠密、稀疏检索拿到两套有序结果，不依赖向量分数，只根据排名重新计算综合得分，平衡语义相似度和关键词匹配，
                // 解决纯向量忽略关键词、纯关键词忽略语义的问题。
                .setQuery(query)
                .setScoreThreshold(0.01f)  // 满分 0.0401的40%
                // 1.4 最终返回数量
                .setLimit(limit)
                // 1.5 检索时带回存储的原文和元数据，否则只能拿到向量和分数
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();

        try {
            List<ScoredPoint> points = qdrantClient.queryAsync(request)
                    .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            // 把 Qdrant 返回的ScoredPoint（检索结果）转回上层业务使用的TextSegment
            return points.stream().map(this::toTextSegment).toList();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant hybrid 检索失败", e);
        }
    }

    private void createHybridCollection(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        // createCollectionAsync：全新创建集合
        qdrantClient.createCollectionAsync(buildCreateCollection(collectionName))
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        createPayloadIndexes(collectionName);
    }

    private void recreateHybridCollection(String collectionName)
            throws ExecutionException, InterruptedException, TimeoutException {
        // recreateCollectionAsync：先删后建
        qdrantClient.recreateCollectionAsync(buildCreateCollection(collectionName))
                .get(RPC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        // 创建完成后调用createPayloadIndexes为过滤字段建立索引，加速userId/diaryId/diaryDate筛选。
        createPayloadIndexes(collectionName);
    }

    /** 构建混合集合配置
     * 这是混合检索的核心定义：告诉 Qdrant 当前集合同时支持稠密向量检索 + 稀疏关键词检索。
     * @param collectionName 集合名称
     * @return 混合集合配置
     */
    private CreateCollection buildCreateCollection(String collectionName) {
        return CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                // 稠密向量配置 DENSE_VECTOR：向量维度（配置读取）、距离度量Cosine余弦相似度
                .setVectorsConfig(VectorsConfig.newBuilder()
                        .setParamsMap(VectorParamsMap.newBuilder()
                                .putMap(DENSE_VECTOR, VectorParams.newBuilder()
                                        .setSize(properties.getEmbeddingDimensions())
                                        .setDistance(Distance.Cosine)
                                        .build())
                                .build())
                        .build())
                // 稀疏向量配置 SPARSE_VECTOR：BM25稀疏向量，使用Idf权重计算
                .setSparseVectorsConfig(SparseVectorConfig.newBuilder()
                        .putMap(SPARSE_VECTOR, SparseVectorParams.newBuilder()
                                .setModifier(Modifier.Idf)
                                .build())
                        .build())
                .build();
    }


    /**
     * 判断集合是否为混合检索集合
     * @param collectionName 集合名称
     * @return 是否为混合检索集合
     */
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

    /**
     * 创建索引
     * 对userId、diaryId、diaryDate 创建 Keyword 类型 payload 索引。
     * @param collectionName 集合名称
     */
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
            // 单日：直接匹配单个diaryDate字符串
            if (temporal.isSingleDay()) {
                builder.addMust(matchKeyword("diaryDate", temporal.startDate().toString()));
                // 多日区间：expandDates遍历区间所有日期，生成日期列表，使用matchKeywords多值匹配
            } else {
                builder.addMust(matchKeywords("diaryDate", expandDates(temporal)));
            }
        });

        return builder.build();
    }


    /**
     * 遍历起止日期，生成区间内每一天的字符串，用于 Qdrant 关键词批量过滤。
     * @param range 时间范围对象
     * @return 生成区间内每一天的字符串
     */
    private List<String> expandDates(TemporalRange range) {
        List<String> dates = new ArrayList<>();
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            dates.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    /**
     * 转换为 PointStruct
     * @param segment 段落
     * @param embedding 向量
     * 把上层业务对象转成 Qdrant 底层存储结构 PointStruct，一个 Point 代表一条向量记录
     * @return PointStruct
     */
    private PointStruct toPointStruct(TextSegment segment, Embedding embedding) {
        String text = segment.text();
        // 构造 Payload 元数据
        Map<String, Value> payload = new HashMap<>();
        payload.put(PAYLOAD_TEXT_KEY, value(text));
        // 把 TextSegment 的 Metadata（userId、diaryId、diaryDate、diaryId 等日记业务字段）
        // 全部存入 payload，用于后续过滤、回显文本
        segment.metadata().toMap().forEach((key, val) -> {
            if (val != null) {
                payload.put(key, value(String.valueOf(val)));
            }
        });

        // 传入原文，服务端自动生成稀疏向量，由Qdrant 内置 BM25 文档模型自动生成，不需要手动计算稀疏向量，
        // Qdrant 内部完成分词、词频、IDF 计算
        Document bm25Document = Document.newBuilder()
                .setModel(BM25_MODEL)
                .setText(text)
                .build();


        // dense：稠密浮点向量，由 Embedding 转换而来
        // bm25：稀疏向量
        return PointStruct.newBuilder()
                .setId(id(UUID.randomUUID()))
                .setVectors(namedVectors(Map.of(
                        DENSE_VECTOR, vector(toFloatList(embedding)),
                        SPARSE_VECTOR, vector(bm25Document)
                )))
                .putAllPayload(payload)
                .build();
    }

    /**
     * 把 Qdrant 返回的ScoredPoint（检索结果）转回上层业务使用的TextSegment
     */
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


    // LangChain4j Embedding 是float[]，Qdrant gRPC 需要List<Float>，做数组转列表
    private List<Float> toFloatList(Embedding embedding) {
        float[] vector = embedding.vector();
        List<Float> floats = new ArrayList<>(vector.length);
        for (float v : vector) {
            floats.add(v);
        }
        return floats;
    }
}
