package com.note.ai.config;

import com.note.constant.RagSettingConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.qdrant")
public class QdrantProperties {

    private String host = "localhost";

    private int port = RagSettingConstant.QDRANT_PORT;

    private String collectionName = "time-note";

    /** 启动时若 collection 非 hybrid 结构则重建（开发/迁移用） */
    private boolean recreateCollection = false;

    /** 启动时从 MySQL 全量重建向量索引 */
    private boolean reindexOnStartup = false;

    private int embeddingDimensions = RagSettingConstant.QDRANT_EMBEDDING_DIMENSIONS;

    /** 混合检索每路预取条数 */
    private int prefetchLimit = RagSettingConstant.QDRANT_PREFETCH_LIMIT;

    /** 合并去重后送入 LLM 的最大日记条数 */
    private int finalLimit = RagSettingConstant.QDRANT_FINAL_LIMIT;

    /** Qdrant 原始召回 chunk 上限（合并前，应大于 finalLimit） */
    private int candidateLimit = RagSettingConstant.QDRANT_CANDIDATE_LIMIT;

    /** 稠密向量预取最低相似度 */
    private float densePrefetchScoreThreshold = RagSettingConstant.QDRANT_DENSE_PREFETCH_SCORE_THRESHOLD;

    /** RRF 融合后的最低得分阈值 */
    private float rrfScoreThreshold = RagSettingConstant.QDRANT_RRF_SCORE_THRESHOLD;
}
