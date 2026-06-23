package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;
import com.note.ai.model.RagSearchResult;
import com.note.ai.model.TemporalRange;
import com.note.ai.store.QdrantHybridStore;
import com.note.config.QdrantProperties;
import com.note.entity.SysDiary;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import com.note.ai.utils.RagContextConsolidator.DiaryContext;

@Component
public class RagUtils {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private QdrantHybridStore qdrantHybridStore;

    @Resource
    private QdrantProperties qdrantProperties;

    @Resource
    private TemporalQueryParser temporalQueryParser;

    private final int CHUNK_SIZE = 500;
    private final int OVERLAP = 100;


    /**
     * 混合检索用户问题，返回检索结果（含时间范围无日记时的直接回复）
     */
    public RagSearchResult searchByUserMessage(String userMessage, String userId) {
        Optional<TemporalRange> dateRange = temporalQueryParser.parse(userMessage);
        String searchQuery = QueryExpansionUtil.expandForSearch(userMessage);
        Response<Embedding> embed = embeddingModel.embed(searchQuery);
        List<TextSegment> textSegments = qdrantHybridStore.hybridSearch(
                searchQuery,
                embed.content(),
                userId,
                qdrantProperties.getCandidateLimit(),
                dateRange
        );

        if (dateRange.isPresent() && textSegments.isEmpty()) {
            return RagSearchResult.directReply(dateRange.get().emptyResultMessage());
        }

        if (textSegments.isEmpty()) {
            return RagSearchResult.noMatchReply();
        }

        List<DiaryContext> diaries = RagContextConsolidator.consolidate(
                textSegments,
                qdrantProperties.getFinalLimit()
        );
        String context = RagContextConsolidator.formatContext(diaries);
        return RagSearchResult.withContext(context, diaries.size());
    }


    public void embeddingSaveTextAndStore(List<TextSegment> textSegments) {
        Response<List<Embedding>> embedAllList = embeddingModel.embedAll(textSegments);
        qdrantHybridStore.upsertSegments(textSegments, embedAllList.content());
    }

    public void embeddingUpdateTextAndStore(SysDiary sysDiary) {
        String diaryIdStr = sysDiary.getId().toString();
        qdrantHybridStore.deleteByDiaryId(diaryIdStr);
        Document document = toDocument(sysDiary);
        List<TextSegment> textSegments = autoSplit(document);
        embeddingSaveTextAndStore(textSegments);
    }


    public void embeddingDelText(String diaryId) {
        qdrantHybridStore.deleteByDiaryId(diaryId);
    }


    public Document toDocument(SysDiary diary) {
        String diaryId = StrUtil.toString(diary.getId());
        String bookId = StrUtil.toString(diary.getBookId());
        String userId = StrUtil.toString(diary.getUserId());
        String diaryDate = StrUtil.toString(diary.getDiaryDate());
        String bookName = StrUtil.trim(diary.getBookName());
        String title = StrUtil.trim(diary.getTitle());
        String city = StrUtil.trim(diary.getName());
        String district = StrUtil.trim(diary.getAdm2());
        String weatherText = StrUtil.trim(diary.getText());
        String temp = StrUtil.trim(diary.getTemp());
        String content = StrUtil.trim(diary.getContent());
        String wordCount = StrUtil.toString(diary.getWordCount());


        String locationLine = StrUtil.isBlank(city) && StrUtil.isBlank(district)
                ? ""
                : StrUtil.format("地点：{}{}", city, district);

        String fullText = StrUtil.format("""
            日记本ID：{}
            日记ID: {}
            用户ID：{}
            日记日期：{}
            日记本名称：{}
            日记标题：{}
            {}
            天气：{}，温度：{}
            日记正文：{}
            日记字数：{}
            """,
                bookId,
                diaryId,
                userId,
                diaryDate,
                bookName,
                title,
                locationLine,
                weatherText,
                temp,
                content,
                wordCount
        );

        Metadata metadata = new Metadata();
        metadata.put("diaryId", diaryId);
        metadata.put("userId", userId);
        metadata.put("bookId", bookId);

        if (StrUtil.isNotBlank(bookName)) metadata.put("bookName", bookName);
        if (StrUtil.isNotBlank(title)) metadata.put("title", title);
        if (StrUtil.isNotBlank(diaryDate)) metadata.put("diaryDate", diaryDate);
        if (StrUtil.isNotBlank(wordCount)) metadata.put("wordCount", wordCount);
        if (StrUtil.isNotBlank(city)) metadata.put("city", city);
        if (StrUtil.isNotBlank(district)) metadata.put("district", district);
        if (StrUtil.isNotBlank(weatherText)) metadata.put("weatherText", weatherText);
        if (StrUtil.isNotBlank(temp)) metadata.put("temperature", temp);

        return Document.document(fullText, metadata);
    }


    public List<TextSegment> autoSplit(Document document) {
        String text = document.text();
        int TEXT_THRESHOLD = 500;
        if (text.length() < TEXT_THRESHOLD) {
            return List.of(TextSegment.from(text, document.metadata()));
        }
        return DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP).split(document);
    }
}
