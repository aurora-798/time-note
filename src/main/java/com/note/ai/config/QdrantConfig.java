package com.note.ai.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(QdrantProperties properties) {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(properties.getHost(), properties.getPort(), false)
                        .build());
    }
}
