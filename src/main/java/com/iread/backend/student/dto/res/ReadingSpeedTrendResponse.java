package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReadingSpeedTrendResponse(
        LocalDate from,
        LocalDate to,
        String unit,
        BigDecimal voiceChangeRate,
        BigDecimal gazeChangeRate,
        List<Point> points
) {
    public record Point(
            LocalDate date,
            BigDecimal voiceSpeed,
            BigDecimal gazeSpeed,
            Long voiceWordCount,
            Long gazeWordCount,
            Long voiceDurationMs,
            Long gazeDurationMs,
            Integer trainingCount
    ) {
    }
}
