package com.note.ai.utils;

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
    private  final int CHUNK_SIZE = 500;
    private  final int OVERLAP = 100;


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
     * @param userId 当前登录用户ID
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
                .queryEmbedding(question)     // 用户提问，自动调用text-embedding-v4向量化
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
    public void embeddingUpdateTextAndStore(Filter filter,SysDiary sysDiary) {
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
        // 拼接日记结构化文本
        String fullText = String.format("""
                日记本ID：%s
                日记ID: %s
                用户ID：%s
                日记日期：%s
                标题：%s
                地点：%s%s
                天气：%s，温度：%s
                日记标题：%s
                日记正文：%s
                日记字数：%s
                """,
                diary.getId(),
                diary.getBookId(),
                diary.getUserId(),
                diary.getDiaryDate(),
                diary.getTitle(),
                diary.getName(), diary.getAdm2(),
                diary.getText(),
                diary.getTemp(),
                diary.getTitle(),
                diary.getContent(),
                diary.getWordCount()
        );

        // 构建元数据
        Metadata metadata = new Metadata()
                // 权限&唯一标识（必选）
                .put("diaryId", diary.getId().toString())
                .put("userId", diary.getUserId().toString())
                .put("bookId", diary.getBookId().toString())
                // 标题筛选
                .put("title", diary.getTitle())
                // 时间筛选
                .put("diaryDate", diary.getDiaryDate().toString())
                // 字数筛选
                .put("wordCount", diary.getWordCount().toString())
                // 地点、天气筛选（建议存入元数据）
                .put("city", diary.getName())
                .put("district", diary.getAdm2())
                .put("weatherText", diary.getText())
                .put("temperature", diary.getTemp());

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
