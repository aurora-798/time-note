package com.note.service;

import com.note.entity.AiChatMessage;
import com.note.entity.vo.chat.AiChatMessageVo;
import com.note.entity.vo.chat.AiChatSessionVo;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

public interface AiChatSessionService {

    int REWRITE_HISTORY_LIMIT = 6;
    int REWRITE_ASSISTANT_TRUNCATE = 800;
    int ASSISTANT_STORE_MAX = 8000;
    int SESSION_LIST_LIMIT = 30;
    int CHAT_MEMORY_LIMIT = 100;

    Long getOrCreateSession(Long userId, Long sessionId);

    /** 供 Query Rewrite 使用的近期消息（只读，不写 Redis） */
    List<AiChatMessage> listRecentMessages(Long sessionId, int limit);

    /** 从 DB 灌入 LangChain4j ChatMemory（会先 clear 再加载） */
    List<AiChatMessage> loadRecentMessages(Long sessionId, MessageWindowChatMemory chatMemory, int limit);

    void appendUserMessage(Long sessionId, Long userId, String content, String searchQuery);

    void appendAssistantMessage(Long sessionId, Long userId, String content);

    void touchSession(Long sessionId);

    List<AiChatMessageVo> listMessages(Long sessionId, Long userId);

    List<AiChatSessionVo> listSessions(Long userId, int limit);

    void deleteSession(Long sessionId, Long userId);

    void updateTitleIfDefault(Long sessionId, String userMessage);

    void clearChatMemory(String sessionId);
}
