package com.iread.backend.student.app.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GrowthAreaResponse(
        int areaId,
        String name,
        int stage,
        String stageName,
        long completedCount,
        int distinctTemplateCount,
        int totalTemplateCount,
        int experiencedCoveragePercent,
        int masteredTemplateCount,
        int masteredCoveragePercent,
        BigDecimal recentAverageAccuracy,
        LocalDateTime updatedAt
) {
}
