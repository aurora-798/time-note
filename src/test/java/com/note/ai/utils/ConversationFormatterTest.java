package com.note.ai.utils;

import com.note.entity.AiChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationFormatterTest {

    @Test
    void formatForRewrite_emptyHistory() {
        assertEquals("（无）", ConversationFormatter.formatForRewrite(List.of()));
        assertEquals("（无）", ConversationFormatter.formatForRewrite(null));
    }

    @Test
    void formatForRewrite_truncatesAssistantReply() {
        AiChatMessage user = new AiChatMessage();
        user.setRole("user");
        user.setContent("我上周去了哪里？");

        AiChatMessage assistant = new AiChatMessage();
        assistant.setRole("assistant");
        assistant.setContent("a".repeat(900));

        String rewriteText = ConversationFormatter.formatForRewrite(List.of(user, assistant));
        assertTrue(rewriteText.startsWith("用户：我上周去了哪里？"));
        assertTrue(rewriteText.contains("助手：" + "a".repeat(800) + "…"));
    }
}
