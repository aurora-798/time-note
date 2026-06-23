package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检索前轻量 query 扩展：补充领域同义表述，提升 BM25 / 向量召回一致性。
 * 仅用于检索，不改变送入 LLM 的原始用户问题。
 */
public final class QueryExpansionUtil {

    private static final Map<String, List<String>> SYNONYM_GROUPS = Map.of(
            "query改写", List.of("query 改写", "查询改写", "查询语句优化", "查询优化", "数据库查询优化"),
            "向量数据库", List.of("向量库", "Qdrant", "向量持久化", "向量持久化改造", "向量检索", "持久化向量"),
            "docker", List.of("Docker", "容器部署"),
            "拖延", List.of("拖延症", "拖延", "没动力", "自卑"),
            "考研", List.of("考研", "放弃考研")
    );

    private QueryExpansionUtil() {
    }

    public static String expandForSearch(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return userMessage;
        }
        String normalized = userMessage.toLowerCase();
        Set<String> extras = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : SYNONYM_GROUPS.entrySet()) {
            List<String> group = entry.getValue();
            boolean matched = group.stream().anyMatch(term -> containsTerm(normalized, userMessage, term));
            if (!matched && containsTerm(normalized, userMessage, entry.getKey())) {
                matched = true;
            }
            if (matched) {
                for (String term : group) {
                    if (!containsTerm(normalized, userMessage, term)) {
                        extras.add(term);
                    }
                }
            }
        }

        if (extras.isEmpty()) {
            return userMessage;
        }
        return userMessage + " " + String.join(" ", extras);
    }

    private static boolean containsTerm(String normalizedMessage, String rawMessage, String term) {
        if (StrUtil.isBlank(term)) {
            return false;
        }
        return normalizedMessage.contains(term.toLowerCase()) || rawMessage.contains(term);
    }
}
