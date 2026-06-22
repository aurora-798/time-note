package com.note.ai.utils;

import com.note.ai.model.TemporalRange;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemporalQueryParser {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Pattern ISO_DATE = Pattern.compile(
            "(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?"
    );

    private static final Pattern CN_DATE = Pattern.compile(
            "(?<!\\d)(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号](?!\\d)"
    );

    private static final Pattern CN_DATE_NO_YEAR = Pattern.compile(
            "(?<!\\d)(\\d{1,2})月(\\d{1,2})[日号](?!\\d)"
    );

    public LocalDate today() {
        return LocalDate.now(SERVER_ZONE);
    }

    public Optional<TemporalRange> parse(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        LocalDate today = today();

        Optional<TemporalRange> absolute = parseAbsoluteDate(query, today);
        if (absolute.isPresent()) {
            return absolute;
        }

        if (contains(query, "大前天")) {
            return Optional.of(singleDay(today.minusDays(3), "大前天"));
        }
        if (contains(query, "前天")) {
            return Optional.of(singleDay(today.minusDays(2), "前天"));
        }
        if (contains(query, "昨天", "昨日")) {
            return Optional.of(singleDay(today.minusDays(1), "昨天"));
        }
        if (contains(query, "今天", "今日")) {
            return Optional.of(singleDay(today, "今天"));
        }
        if (contains(query, "上周", "上星期", "上一周")) {
            return Optional.of(weekRange(today, -1, "上周"));
        }
        if (contains(query, "本周", "这周", "本星期")) {
            return Optional.of(weekRange(today, 0, "本周"));
        }
        if (contains(query, "上月", "上个月")) {
            return Optional.of(monthRange(today, -1, "上月"));
        }
        if (contains(query, "本月", "这个月")) {
            return Optional.of(monthRange(today, 0, "本月"));
        }

        return Optional.empty();
    }

    private Optional<TemporalRange> parseAbsoluteDate(String query, LocalDate today) {
        Matcher iso = ISO_DATE.matcher(query);
        if (iso.find()) {
            return toSingleDayRange(iso.group(1), iso.group(2), iso.group(3));
        }

        Matcher cn = CN_DATE.matcher(query);
        if (cn.find()) {
            return toSingleDayRange(cn.group(1), cn.group(2), cn.group(3));
        }

        Matcher cnNoYear = CN_DATE_NO_YEAR.matcher(query);
        if (cnNoYear.find()) {
            return toSingleDayRange(String.valueOf(today.getYear()), cnNoYear.group(1), cnNoYear.group(2));
        }

        return Optional.empty();
    }

    private Optional<TemporalRange> toSingleDayRange(String year, String month, String day) {
        try {
            LocalDate date = LocalDate.of(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    Integer.parseInt(day)
            );
            return Optional.of(singleDay(date, date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private TemporalRange singleDay(LocalDate date, String label) {
        return new TemporalRange(date, date, label);
    }

    private TemporalRange weekRange(LocalDate today, int weekOffset, String label) {
        LocalDate monday = today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset);
        return new TemporalRange(monday, monday.plusDays(6), label);
    }

    private TemporalRange monthRange(LocalDate today, int monthOffset, String label) {
        LocalDate first = today.withDayOfMonth(1).plusMonths(monthOffset);
        return new TemporalRange(first, first.withDayOfMonth(first.lengthOfMonth()), label);
    }

    private boolean contains(String query, String... keywords) {
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
