package com.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.qdrant")
public class QdrantProperties {

    private String host = "localhost";

    private int port = 6334;

    private String collectionName = "time-note";

    /** 启动时若 collection 非 hybrid 结构则重建（开发/迁移用） */
    private boolean recreateCollection = false;

    /** 启动时从 MySQL 全量重建向量索引 */
    private boolean reindexOnStartup = false;

    private int embeddingDimensions = 1536;

    private int prefetchLimit = 20;

    private int finalLimit = 5;
}
