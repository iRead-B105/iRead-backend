package com.iread.backend.report.admin.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReportResponse(
        Long reportId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        long learningDays,
        long totalTrainingTime,
        long completedTrainingCount,
        BigDecimal averageAccuracy,
        BigDecimal averageReadingSpeed,
        List<ReportSnapshot.Growth> growthByPeriod,
        Map<String, BigDecimal> achievementByDomain,
        List<String> frequentErrorWords,
        List<String> improvedPatterns,
        List<String> persistentDifficulties,
        ReportSnapshot.GazeAnalysis gazeAnalysis,
        String teacherMemo
) {
}
