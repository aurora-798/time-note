package com.note.service;

import com.note.entity.vo.chat.AiChatMessageVo;
import com.note.entity.vo.chat.AiChatSessionVo;
import com.note.entity.vo.chat.AiChatStreamResult;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiChatService {

    /**
     * 单轮聊天（GET 兼容）
     */
    Flux<String> chat(String userId, String userMessage);

    /**
     * 多轮 RAG 聊天
     */
    AiChatStreamResult chatMultiTurn(String userId, String userMessage, Long sessionId);

    List<AiChatSessionVo> listSessions(String userId);

    List<AiChatMessageVo> listSessionMessages(String userId, Long sessionId);

    void deleteSession(String userId, Long sessionId);
}
