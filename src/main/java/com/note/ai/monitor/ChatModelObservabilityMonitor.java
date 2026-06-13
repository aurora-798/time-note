package com.note.ai.monitor;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelObservabilityMonitor {

    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            private static final Logger log = LoggerFactory.getLogger(ChatModelListener.class);

            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                ChatRequest chatRequest = requestContext.chatRequest();
                ChatRequestParameters parameters = chatRequest.parameters();
                log.info("modelName:{}", parameters.modelName());

            }


            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                ChatResponse chatResponse = responseContext.chatResponse();
                ChatResponseMetadata metadata = chatResponse.metadata();
                TokenUsage tokenUsage = metadata.tokenUsage();
                log.info("inputTokenCount：{}", tokenUsage.inputTokenCount());
                log.info("outputTokenCount：{}", tokenUsage.outputTokenCount());
                log.info("totalTokenCount：{}", tokenUsage.totalTokenCount());
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                Throwable error = errorContext.error();
                error.printStackTrace();
            }
        };
    }
}
