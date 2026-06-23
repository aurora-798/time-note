package com.note.entity.request.chat;

import lombok.Data;

@Data
public class AiChatRequest {

    private String userId;
    private String userMessage;
    private String sessionId;
}
