package com.note.entity.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.publisher.Flux;

@Data
@AllArgsConstructor
public class AiChatStreamResult {

    private Long sessionId;
    private Flux<String> stream;
}
