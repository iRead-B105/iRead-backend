package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReadingSpeedTrendResponse(
        LocalDate from,
        LocalDate to,
        String unit,
        String calculationVersion,
        BigDecimal voiceChangeRate,
        BigDecimal gazeChangeRate,
        List<Point> points
) {
    public record Point(
            LocalDate date,
            BigDecimal voiceSpeed,
            BigDecimal gazeSpeed,
            Long correctWordCount,
            Long voiceDurationMs,
            Long gazeWordCount,
            Long gazeDurationMs,
            Integer trainingCount
    ) {
    }
}
