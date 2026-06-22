package com.note.ai.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record TemporalRange(LocalDate startDate, LocalDate endDate, String label) {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy年M月d日");

    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    public String emptyResultMessage() {
        if (isSingleDay()) {
            return "「" + label + "」（" + startDate.format(DISPLAY) + "）还没有日记记录。";
        }
        return "「" + label + "」（" + startDate.format(DISPLAY) + " 至 " + endDate.format(DISPLAY) + "）还没有日记记录。";
    }
}
