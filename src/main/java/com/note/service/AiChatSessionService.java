package com.note.service;

import com.note.constant.RagSettingConstant;
import com.note.entity.AiChatMessage;
import com.note.entity.vo.chat.AiChatMessageVo;
import com.note.entity.vo.chat.AiChatSessionVo;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

public interface AiChatSessionService {

    int REWRITE_HISTORY_LIMIT = RagSettingConstant.QUERY_REWRITE_HISTORY_LIMIT;
    int REWRITE_ASSISTANT_TRUNCATE = RagSettingConstant.QUERY_REWRITE_ASSISTANT_TRUNCATE;
    int ASSISTANT_STORE_MAX = RagSettingConstant.RAG_ASSISTANT_STORE_MAX;
    int SESSION_LIST_LIMIT = 30;
    int CHAT_MEMORY_LIMIT = RagSettingConstant.RAG_CHAT_MEMORY_LIMIT;

    Long getOrCreateSession(Long userId, Long sessionId);

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
