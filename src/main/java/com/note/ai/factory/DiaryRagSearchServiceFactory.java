package com.note.ai.factory;

import com.note.ai.service.DiaryRagSearchService;
import com.note.service.AiChatSessionService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiaryRagSearchServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private AiChatSessionService aiChatSessionService;

    /**
     * 多轮会话：Redis ChatMemory + 从 DB 灌入历史
     */
    public DiaryRagSearchService createWithMemory(String sessionId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(sessionId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(AiChatSessionService.CHAT_MEMORY_LIMIT)
                .build();

        aiChatSessionService.loadRecentMessages(
                Long.valueOf(sessionId), chatMemory, AiChatSessionService.CHAT_MEMORY_LIMIT);

        return AiServices.builder(DiaryRagSearchService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 单轮 GET 兼容：无会话记忆
     */
    public DiaryRagSearchService createStateless() {
        return AiServices.builder(DiaryRagSearchService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
