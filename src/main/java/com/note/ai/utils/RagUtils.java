package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;
import com.note.ai.store.QdrantHybridStore;
import com.note.config.QdrantProperties;
import com.note.entity.SysDiary;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagUtils {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private QdrantHybridStore qdrantHybridStore;

    @Resource
    private QdrantProperties qdrantProperties;

    // 长文本分割参数
    private final int CHUNK_SIZE = 500;
    private final int OVERLAP = 100;


    /**
     * 混合检索用户问题，返回答案上下文
     */
    public String searchAnswerByUserMessage(String userMessage, String userId) {
        Response<Embedding> embed = embeddingModel.embed(userMessage);
        List<TextSegment> textSegments = qdrantHybridStore.hybridSearch(
                userMessage,
                embed.content(),
                userId,
                qdrantProperties.getFinalLimit()
        );
        if (textSegments.isEmpty()) {
            return "没有找到和你问题相关的日记内容";
        }
        StringBuilder context = new StringBuilder();
        for (TextSegment seg : textSegments) {
            context.append(seg.text());
            context.append("\n-------------------------\n");
        }
        return context.toString();
    }


    /**
     * 保存向量数据库（dense + BM25 sparse）
     */
    public void embeddingSaveTextAndStore(List<TextSegment> textSegments) {
        Response<List<Embedding>> embedAllList = embeddingModel.embedAll(textSegments);
        qdrantHybridStore.upsertSegments(textSegments, embedAllList.content());
    }

    /**
     * 更新向量数据库
     */
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


    /**
     * 将单条日记实体转为LangChain4j Document
     */
    public Document toDocument(SysDiary diary) {
        String diaryId = StrUtil.toString(diary.getId());
        String bookId = StrUtil.toString(diary.getBookId());
        String userId = StrUtil.toString(diary.getUserId());
        String diaryDate = StrUtil.toString(diary.getDiaryDate());
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
            标题：{}
            {}
            天气：{}，温度：{}
            日记正文：{}
            日记字数：{}
            """,
                bookId,
                diaryId,
                userId,
                diaryDate,
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

        if (StrUtil.isNotBlank(title)) metadata.put("title", title);
        if (StrUtil.isNotBlank(diaryDate)) metadata.put("diaryDate", diaryDate);
        if (StrUtil.isNotBlank(wordCount)) metadata.put("wordCount", wordCount);
        if (StrUtil.isNotBlank(city)) metadata.put("city", city);
        if (StrUtil.isNotBlank(district)) metadata.put("district", district);
        if (StrUtil.isNotBlank(weatherText)) metadata.put("weatherText", weatherText);
        if (StrUtil.isNotBlank(temp)) metadata.put("temperature", temp);

        return Document.document(fullText, metadata);
    }


    /**
     *  文档自适应分块
     */
    public List<TextSegment> autoSplit(Document document) {
        String text = document.text();
        int TEXT_THRESHOLD = 500;
        if (text.length() < TEXT_THRESHOLD) {
            return List.of(TextSegment.from(text, document.metadata()));
        }
        return DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP).split(document);
    }
}
