package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;
import com.note.entity.SysDiary;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagUtils {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    // 长文本分割参数
    private final int CHUNK_SIZE = 500;
    private final int OVERLAP = 100;


    /**
     * 检索用户问题，返回答案
     * @param userMessage 用户问题
     * @param userId 当前登录用户 ID
     * @return 检索到的答案
     */
    public String searchAnswerByUserMessage(String userMessage,String userId) {
        Response<Embedding> embed = embeddingModel.embed(userMessage);
        List<TextSegment> textSegments = searchDiarySegment(userId, embed.content(), 5, 0.7);
        if (textSegments.isEmpty()) {
            return "没有找到和你问题相关的日记内容";
        }
        // 拼接所有日记片段
        StringBuilder context = new StringBuilder();
        for (TextSegment seg : textSegments) {
            context.append(seg.text());
            context.append("\n-------------------------\n");
        }
        return context.toString();
    }


    /**
     * 根据用户问题，检索当前用户相关日记片段
     * @param userId 当前登录用户 ID
     * @param question 用户提问
     * @param maxResult 最多返回几条片段
     * @param minScore 相似度阈值（0~1，越高越相关）
     * @return 匹配的日记文本片段
     */
    public List<TextSegment> searchDiarySegment(String userId, Embedding question,
                                                int maxResult, double minScore) {
        // 1. 构造过滤条件：只查询当前登录用户的日记 过滤逻辑：userId = 当前用户ID
        Filter userFilter = new IsEqualTo("userId", userId);

        // 2. 构建检索请求
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(question)     // 用户提问，自动调用 text-embedding-v4 向量化
                .filter(userFilter)     // 用户隔离，看不到别人日记
                .maxResults(maxResult)   // 召回条数
                .minScore(minScore)      // 低于该相似度直接丢弃
                .build();

        // 3. 执行向量检索
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // 4. 提取所有匹配到的TextSegment（内含文本+元数据）
        return searchResult.matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }




    /**
     * 保存向量数据库
     * 自动调用配置的text-embedding-v4进行向量化
     */
    public void embeddingSaveTextAndStore(List<TextSegment> textSegments) {
        Response<List<Embedding>> embedAllList = embeddingModel.embedAll(textSegments);
        // 向量+文本片段成对存入，保留元数据和原文
        embeddingStore.addAll(embedAllList.content(),textSegments);
    }

    /**
     * 更新向量数据库
     */
    public void embeddingUpdateTextAndStore(SysDiary sysDiary) {
        String diaryIdStr = sysDiary.getId().toString();
        // 根据 diaryId 删除全部旧向量片段
        Filter filter = new IsEqualTo("diaryId", diaryIdStr);
        embeddingStore.removeAll(filter);
        // 1. 实体转带元数据文档
        Document document = toDocument(sysDiary);
        // 2. 自适应分块
        List<TextSegment> textSegments = autoSplit(document);
        // 3. 批量保存向量+文本片段
        embeddingSaveTextAndStore(textSegments);
    }


    public void embeddingDelText(String diaryId) {
        Filter filter = new IsEqualTo("diaryId", diaryId);
        embeddingStore.removeAll(filter);
    }


    /**
     * 将单条日记实体转为LangChain4j Document
     */
    public Document toDocument(SysDiary diary) {
        // 1. 统一使用StrUtil处理所有字段，null/空串/空白全部转为空字符串
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

        // 2. 地点拼接优化：无地点时不输出多余内容
        String locationLine = StrUtil.isBlank(city) && StrUtil.isBlank(district)
                ? ""
                : StrUtil.format("地点：{}{}", city, district);

        // 3. 拼接完整文本，消除null字样、重复标题、多余空行
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

        // 4. 构建元数据，仅存入非空字段，减少无效过滤干扰
        Metadata metadata = new Metadata();
        // 主键必存（ID不可能为空，业务约束）
        metadata.put("diaryId", diaryId);
        metadata.put("userId", userId);
        metadata.put("bookId", bookId);

        // 有值才存入元数据，避免存空字符串干扰检索筛选
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
        // 短日记：不分割
        // 短文本阈值
        int TEXT_THRESHOLD = 500;
        if (text.length() < TEXT_THRESHOLD) {
            return List.of(TextSegment.from(text, document.metadata()));
        }
        // 长日记：递归分割
        return DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP).split(document);
    }
}
