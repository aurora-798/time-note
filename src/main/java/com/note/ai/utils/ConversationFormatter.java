package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;
import com.note.entity.AiChatMessage;
import com.note.service.AiChatSessionService;

import java.util.List;

public final class ConversationFormatter {

    private static final String EMPTY_PLACEHOLDER = "（无）";

    private ConversationFormatter() {
    }

    public static String formatForRewrite(List<AiChatMessage> history) {
        return format(history, AiChatSessionService.REWRITE_ASSISTANT_TRUNCATE);
    }

    private static String format(List<AiChatMessage> history, int assistantTruncate) {
        if (history == null || history.isEmpty()) {
            return EMPTY_PLACEHOLDER;
        }
        StringBuilder sb = new StringBuilder();
        for (AiChatMessage message : history) {
            String role = message.getRole();
            String label = "user".equals(role) ? "用户" : "助手";
            String content = StrUtil.nullToEmpty(message.getContent());
            if ("assistant".equals(role)) {
                content = truncate(content, assistantTruncate);
            }
            sb.append(label).append("：").append(content).append('\n');
        }
        return sb.toString().trim();
    }

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }
}
