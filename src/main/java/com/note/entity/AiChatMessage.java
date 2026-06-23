package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
@Schema(description = "AI 聊天消息")
public class AiChatMessage {

    @TableId(type = IdType.AUTO)
    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "角色：user | assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "检索改写 query")
    private String searchQuery;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
