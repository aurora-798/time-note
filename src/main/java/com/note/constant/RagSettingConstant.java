package com.note.constant;

/**
 * RAG 链路数字类配置常量
 */
public interface RagSettingConstant {

    // ==================== langchain4j.qdrant ====================

    /** Qdrant gRPC 端口，对应 {@code langchain4j.qdrant.port} */
    int QDRANT_PORT = 6334;

    /** 稠密向量维度，对应 {@code langchain4j.qdrant.embedding-dimensions} */
    int QDRANT_EMBEDDING_DIMENSIONS = 1536;

    /** 混合检索每路预取条数，对应 {@code langchain4j.qdrant.prefetch-limit} */
    int QDRANT_PREFETCH_LIMIT = 50;

    /** 融合后 chunk 上限（合并日记前），对应 {@code langchain4j.qdrant.candidate-limit} */
    int QDRANT_CANDIDATE_LIMIT = 80;

    /** 送入 LLM 的最大日记条数，对应 {@code langchain4j.qdrant.final-limit} */
    int QDRANT_FINAL_LIMIT = 5;

    /** 稠密向量预取最低相似度，对应 {@code langchain4j.qdrant.dense-prefetch-score-threshold} */
    float QDRANT_DENSE_PREFETCH_SCORE_THRESHOLD = 0.2f;

    /** RRF 融合后最低得分，对应 {@code langchain4j.qdrant.rrf-score-threshold} */
    float QDRANT_RRF_SCORE_THRESHOLD = 0.015f;

    // ==================== Qdrant 混合检索（代码层） ====================

    /** 混合检索 RRF 融合的 k 参数 */
    int QDRANT_HYBRID_RRF_K = 60;

    /** RRF 第一路（BM25）权重 */
    float QDRANT_HYBRID_RRF_WEIGHT_BM25 = 1.0f;

    /** RRF 第二路（Dense）权重 */
    float QDRANT_HYBRID_RRF_WEIGHT_DENSE = 1.0f;

    /** Qdrant gRPC 调用超时（秒） */
    int QDRANT_RPC_TIMEOUT_SECONDS = 30;

    // ==================== langchain4j.rerank（预留） ====================

    /** Rerank 最低保留分，对应 {@code langchain4j.rerank.min-score} */
    double RERANK_MIN_SCORE = 0.40;

    /** 单条 passage 最大字符数，对应 {@code langchain4j.rerank.max-passage-chars} */
    int RERANK_MAX_PASSAGE_CHARS = 1500;

    // ==================== 日记入库分块 ====================

    /** 长文本分块大小（字符） */
    int DIARY_CHUNK_SIZE = 500;

    /** 相邻分块重叠长度（字符） */
    int DIARY_CHUNK_OVERLAP = 100;

    /** 低于该字数整篇入库，不再切分 */
    int DIARY_WHOLE_TEXT_THRESHOLD = 500;

    // ==================== Query 补全 ====================

    /** 补全 query 时读取的最近对话轮数上限 */
    int QUERY_REWRITE_HISTORY_LIMIT = 6;

    /** 补全 prompt 中助手历史回复最大保留长度（字符） */
    int QUERY_REWRITE_ASSISTANT_TRUNCATE = 800;

    // ==================== RAG 对话会话 ====================

    /** LangChain4j ChatMemory 窗口内最大消息条数 */
    int RAG_CHAT_MEMORY_LIMIT = 100;

    /** 助手回复落库最大字符数 */
    int RAG_ASSISTANT_STORE_MAX = 8000;
}
