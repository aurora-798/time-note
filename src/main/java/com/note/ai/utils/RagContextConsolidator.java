package com.note.ai.utils;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检索后处理：按 diaryId 合并 chunk、结构化上下文，避免同一日记多段割裂进 LLM。
 */
public final class RagContextConsolidator {

    private static final String SCORE_KEY = "_retrievalScore";
    private static final Pattern BODY_MARKER = Pattern.compile("日记正文：");

    private static final double SHORT_TEXT_SCORE_FACTOR = 0.85;
    private static final int SHORT_TEXT_WORD_THRESHOLD = 50;

    private RagContextConsolidator() {
    }

    public record DiaryContext(
            String diaryId,
            String bookName,
            String title,
            String diaryDate,
            String city,
            String district,
            String weatherText,
            String temperature,
            String wordCount,
            String bodyText,
            double bestScore
    ) {
    }

    /**
     * 按 diaryId 分组，合并同日记多 chunk，按最佳检索分排序后截断。
     */
    public static List<DiaryContext> consolidate(List<TextSegment> segments, int maxDiaries) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        Map<String, List<TextSegment>> grouped = new LinkedHashMap<>();
        for (TextSegment segment : segments) {
            String diaryId = metadataString(segment, "diaryId");
            if (StrUtil.isBlank(diaryId)) {
                diaryId = "unknown-" + System.identityHashCode(segment);
            }
            grouped.computeIfAbsent(diaryId, k -> new ArrayList<>()).add(segment);
        }

        List<DiaryContext> diaries = grouped.entrySet().stream()
                .map(entry -> toDiaryContext(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(DiaryContext::bestScore).reversed())
                .limit(Math.max(maxDiaries, 1))
                .toList();

        return diaries;
    }

    public static String formatContext(List<DiaryContext> diaries) {
        if (diaries.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        for (DiaryContext diary : diaries) {
            context.append("=== 日记记录 ===\n");
            if (StrUtil.isNotBlank(diary.bookName())) {
                context.append("日记本名称：").append(diary.bookName()).append('\n');
            }
            if (StrUtil.isNotBlank(diary.title())) {
                context.append("日记标题：").append(diary.title()).append('\n');
            }
            if (isPlanLikeEntry(diary)) {
                context.append("记录类型：开发计划（正文未明确完成态，总结时不得写「已完成」）\n");
            } else if (isProgressLikeEntry(diary)) {
                context.append("记录类型：开发进度\n");
            }
            if (StrUtil.isNotBlank(diary.diaryDate())) {
                context.append("日记日期：").append(diary.diaryDate()).append('\n');
            }
            String location = formatLocation(diary.city(), diary.district());
            if (StrUtil.isNotBlank(location)) {
                context.append("地点：").append(location).append('\n');
            }
            String weatherLine = formatWeather(diary.weatherText(), diary.temperature());
            if (StrUtil.isNotBlank(weatherLine)) {
                context.append("天气：").append(weatherLine).append('\n');
            }
            if (StrUtil.isNotBlank(diary.wordCount())) {
                context.append("日记字数：").append(diary.wordCount()).append('\n');
            }
            context.append("日记正文：").append(diary.bodyText()).append('\n');
            context.append("=== 日记记录结束 ===\n");
        }
        return context.toString().trim();
    }

    private static DiaryContext toDiaryContext(String diaryId, List<TextSegment> chunks) {
        double bestScore = chunks.stream()
                .mapToDouble(RagContextConsolidator::segmentScore)
                .max()
                .orElse(0D);

        TextSegment primary = chunks.stream()
                .max(Comparator.comparingInt(seg -> seg.text() == null ? 0 : seg.text().length()))
                .orElse(chunks.get(0));

        String bookName = firstNonBlank(chunks, "bookName");
        String title = firstNonBlank(chunks, "title");
        String diaryDate = firstNonBlank(chunks, "diaryDate");

        if (StrUtil.isBlank(bookName)) {
            bookName = extractInlineField(primary.text(), "日记本名称：");
        }
        if (StrUtil.isBlank(title)) {
            title = extractInlineField(primary.text(), "日记标题：");
        }
        if (StrUtil.isBlank(diaryDate)) {
            diaryDate = extractInlineField(primary.text(), "日记日期：");
        }

        String city = firstNonBlank(chunks, "city");
        String district = firstNonBlank(chunks, "district");
        String weatherText = firstNonBlank(chunks, "weatherText");
        String temperature = firstNonBlank(chunks, "temperature");
        String wordCount = firstNonBlank(chunks, "wordCount");

        if (StrUtil.isBlank(city) && StrUtil.isBlank(district)) {
            String inlineLocation = extractInlineField(primary.text(), "地点：");
            if (StrUtil.isNotBlank(inlineLocation)) {
                city = inlineLocation;
            }
        }
        if (StrUtil.isBlank(wordCount)) {
            wordCount = extractInlineField(primary.text(), "日记字数：");
        }

        String bodyText = mergeBodyTexts(chunks);
        bestScore = applyLengthPenalty(bestScore, wordCount);

        return new DiaryContext(
                diaryId, bookName, title, diaryDate,
                city, district, weatherText, temperature, wordCount,
                bodyText, bestScore
        );
    }

    private static String formatLocation(String city, String district) {
        if (StrUtil.isBlank(city) && StrUtil.isBlank(district)) {
            return "";
        }
        return StrUtil.nullToEmpty(city) + StrUtil.nullToEmpty(district);
    }

    private static String formatWeather(String weatherText, String temperature) {
        if (StrUtil.isBlank(weatherText) && StrUtil.isBlank(temperature)) {
            return "";
        }
        if (StrUtil.isBlank(weatherText)) {
            return "温度：" + temperature;
        }
        if (StrUtil.isBlank(temperature)) {
            return weatherText;
        }
        return weatherText + "，温度：" + temperature;
    }

    private static double applyLengthPenalty(double score, String wordCount) {
        int count = parseWordCount(wordCount);
        if (count >= 0 && count < SHORT_TEXT_WORD_THRESHOLD) {
            return score * SHORT_TEXT_SCORE_FACTOR;
        }
        return score;
    }

    private static int parseWordCount(String wordCount) {
        if (StrUtil.isBlank(wordCount)) {
            return -1;
        }
        try {
            return Integer.parseInt(wordCount.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String mergeBodyTexts(List<TextSegment> chunks) {
        List<String> bodies = chunks.stream()
                .map(RagContextConsolidator::extractBodyText)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        if (bodies.isEmpty()) {
            return chunks.stream()
                    .map(TextSegment::text)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse("");
        }

        String merged = bodies.get(0);
        for (int i = 1; i < bodies.size(); i++) {
            String candidate = bodies.get(i);
            if (!merged.contains(candidate)) {
                merged = merged + "\n" + candidate;
            }
        }
        return merged.trim();
    }

    private static String extractBodyText(TextSegment segment) {
        String text = StrUtil.nullToEmpty(segment.text());
        Matcher matcher = BODY_MARKER.matcher(text);
        if (!matcher.find()) {
            return text.trim();
        }
        String body = text.substring(matcher.end()).trim();
        int wordCountIdx = body.indexOf("日记字数：");
        if (wordCountIdx >= 0) {
            body = body.substring(0, wordCountIdx).trim();
        }
        return body;
    }

    private static String firstNonBlank(List<TextSegment> chunks, String key) {
        for (TextSegment chunk : chunks) {
            String value = metadataString(chunk, key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String metadataString(TextSegment segment, String key) {
        if (segment.metadata() == null) {
            return "";
        }
        return StrUtil.trim(segment.metadata().getString(key));
    }

    private static double segmentScore(TextSegment segment) {
        String raw = metadataString(segment, SCORE_KEY);
        if (StrUtil.isBlank(raw)) {
            return 0D;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    private static String extractInlineField(String text, String label) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        int start = text.indexOf(label);
        if (start < 0) {
            return "";
        }
        start += label.length();
        int end = text.indexOf('\n', start);
        if (end < 0) {
            end = text.length();
        }
        return text.substring(start, end).trim();
    }

    static boolean isPlanLikeEntry(DiaryContext diary) {
        String title = StrUtil.nullToEmpty(diary.title());
        if (StrUtil.containsAny(title, "计划", "预完成", "待办")) {
            return true;
        }
        String body = StrUtil.nullToEmpty(diary.bodyText());
        if (containsExplicitCompletion(body)) {
            return false;
        }
        int wc = parseWordCount(diary.wordCount());
        return wc >= 0 && wc < SHORT_TEXT_WORD_THRESHOLD;
    }

    static boolean isProgressLikeEntry(DiaryContext diary) {
        String title = StrUtil.nullToEmpty(diary.title());
        return StrUtil.containsAny(title, "进度", "开发记录", "开发日志");
    }

    private static boolean containsExplicitCompletion(String body) {
        return StrUtil.containsAny(body,
                "已经完成", "已完成", "做完了", "今日完成", "今天完成",
                "完成存储", "完成检索", "完成了接入", "完成了存储");
    }
}
