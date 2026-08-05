package com.iread.backend.student.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReadingMetricSummary(
        String calculationVersion,
        String accuracyUnit,
        String readingSpeedUnit,
        BigDecimal averageAccuracy,
        BigDecimal averageReadingSpeed,
        List<DailyMetric> dailyMetrics
) {
    public ReadingMetricSummary {
        dailyMetrics = List.copyOf(dailyMetrics);
    }

    public record DailyMetric(
            LocalDate date,
            BigDecimal accuracy,
            BigDecimal readingSpeed
    ) {
    }
}
