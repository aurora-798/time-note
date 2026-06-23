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
 * <p>
 * 不负责排序与相关度重算（由检索层 / 未来 Rerank 负责）；调用方在 rerank 之后 {@link #limit} 截断。
 */
public final class RagContextConsolidator {

    private static final String SCORE_KEY = "_retrievalScore";
    private static final String INDEX_KEY = "index";
    private static final int MISSING_INDEX = Integer.MAX_VALUE;
    private static final Pattern BODY_MARKER = Pattern.compile("日记正文：");

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
            double retrievalScore
    ) {
        public DiaryContext withRetrievalScore(double score) {
            return new DiaryContext(
                    diaryId, bookName, title, diaryDate,
                    city, district, weatherText, temperature, wordCount,
                    bodyText, score
            );
        }
    }

    /**
     * 按 diaryId 合并 chunk，日记列表顺序与输入 segments 首次出现顺序一致（即检索排序）。
     */
    public static List<DiaryContext> consolidate(List<TextSegment> segments) {
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

        return grouped.entrySet().stream()
                .map(entry -> toDiaryContext(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Rerank 之后截断送入 LLM 的日记条数。 */
    public static List<DiaryContext> limit(List<DiaryContext> diaries, int maxDiaries) {
        if (diaries == null || diaries.isEmpty()) {
            return List.of();
        }
        return diaries.stream().limit(Math.max(maxDiaries, 1)).toList();
    }

    public static String formatContext(List<DiaryContext> diaries) {
        if (diaries == null || diaries.isEmpty()) {
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

    // <diaryId,与 diaryId 有关的分片>
    private static DiaryContext toDiaryContext(String diaryId, List<TextSegment> chunks) {
        // 取得最大得分
        double retrievalScore = chunks.stream()
                .mapToDouble(RagContextConsolidator::segmentScore)
                .max()
                .orElse(0D);

        // 元数据优先用 index 最小的 chunk
        TextSegment primary = primaryChunk(chunks);

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

        return new DiaryContext(
                diaryId, bookName, title, diaryDate,
                city, district, weatherText, temperature, wordCount,
                mergeBodyTexts(chunks), retrievalScore
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

    // 按 index 顺序合并分片
    private static String mergeBodyTexts(List<TextSegment> chunks) {
        String merged = "";
        for (TextSegment chunk : sortChunksByIndex(chunks)) {
            String body = extractBodyText(chunk);
            if (StrUtil.isNotBlank(body)) {
                merged = appendBodyWithOverlap(merged, body);
            }
        }
        if (StrUtil.isNotBlank(merged)) {
            return merged.trim();
        }
        return chunks.stream()
                .map(TextSegment::text)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("");
    }


    // 元数据优先用 index 最小的 chunk
    private static TextSegment primaryChunk(List<TextSegment> chunks) {
        return chunks.stream()
                .min(Comparator.comparingInt(RagContextConsolidator::chunkIndex))
                .filter(seg -> chunkIndex(seg) != MISSING_INDEX)
                .orElseGet(() -> chunks.stream()
                        .max(Comparator.comparingInt(seg -> seg.text() == null ? 0 : seg.text().length()))
                        .orElse(chunks.getFirst()));
    }

    private static List<TextSegment> sortChunksByIndex(List<TextSegment> chunks) {
        boolean hasIndex = chunks.stream().anyMatch(seg -> chunkIndex(seg) != MISSING_INDEX);
        if (hasIndex) {
            return chunks.stream()
                    .sorted(Comparator.comparingInt(RagContextConsolidator::chunkIndex))
                    .toList();
        }
        return chunks.stream()
                .sorted(Comparator.comparingInt((TextSegment seg) ->
                        seg.text() == null ? 0 : seg.text().length()).reversed())
                .toList();
    }

    private static String appendBodyWithOverlap(String merged, String body) {
        if (StrUtil.isBlank(merged)) {
            return body;
        }
        if (merged.contains(body)) {
            return merged;
        }
        if (body.contains(merged)) {
            return body;
        }
        int maxOverlap = Math.min(merged.length(), body.length());
        for (int len = maxOverlap; len > 0; len--) {
            if (merged.endsWith(body.substring(0, len))) {
                return merged + body.substring(len);
            }
        }
        return merged + body;
    }

    private static int chunkIndex(TextSegment segment) {
        String raw = metadataString(segment, INDEX_KEY);
        if (StrUtil.isBlank(raw)) {
            return MISSING_INDEX;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return MISSING_INDEX;
        }
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
}
