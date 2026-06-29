package com.note.ai.rerank;

import cn.hutool.core.util.StrUtil;
import com.note.ai.utils.RagContextConsolidator.DiaryContext;

/**
 * 将日记上下文转为 rerank 模型可读的 passage 文本。
 */
public final class DiaryPassageBuilder {

    private DiaryPassageBuilder() {
    }

    public static String build(DiaryContext diary, int maxChars) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(diary.bookName())) {
            sb.append("日记本名称：").append(diary.bookName()).append('\n');
        }
        if (StrUtil.isNotBlank(diary.title())) {
            sb.append("日记标题：").append(diary.title()).append('\n');
        }
        if (StrUtil.isNotBlank(diary.diaryDate())) {
            sb.append("日记日期：").append(diary.diaryDate()).append('\n');
        }
        String location = formatLocation(diary.city(), diary.district());
        if (StrUtil.isNotBlank(location)) {
            sb.append("地点：").append(location).append('\n');
        }
        String weatherLine = formatWeather(diary.weatherText(), diary.temperature());
        if (StrUtil.isNotBlank(weatherLine)) {
            sb.append("天气：").append(weatherLine).append('\n');
        }
        if (StrUtil.isNotBlank(diary.wordCount())) {
            sb.append("日记字数：").append(diary.wordCount()).append('\n');
        }
        sb.append("日记正文：").append(StrUtil.nullToEmpty(diary.bodyText()));

        String passage = sb.toString().trim();
        if (maxChars > 0 && passage.length() > maxChars) {
            return passage.substring(0, maxChars);
        }
        return passage;
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
}
