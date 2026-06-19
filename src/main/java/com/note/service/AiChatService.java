package com.note.service;

public interface AiChatService {

    /**
     * 聊天
     * @param userId 用户 ID
     * @param userMessage 用户消息
     * @return 聊天结果
     */
    String chat(String userId,String userMessage);
}
