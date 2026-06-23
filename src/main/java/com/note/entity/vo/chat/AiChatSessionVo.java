package com.note.entity.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSessionVo {

    private String sessionId;
    private String title;
    private LocalDateTime updateTime;
    private Long messageCount;
}
