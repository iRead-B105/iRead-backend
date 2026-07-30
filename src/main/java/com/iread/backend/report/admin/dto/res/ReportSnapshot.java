package com.iread.backend.report.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportSnapshot(
        long learningDays,
        long totalTrainingTimeMinutes,
        long completedTrainingCount,
        BigDecimal averageAccuracy,
        BigDecimal averageReadingSpeed,
        String readingSpeedUnit,
        List<Growth> growthHistory,
        List<AreaAchievement> areaAchievements,
        List<IncorrectWord> frequentlyIncorrectWords,
        List<String> improvedPatterns,
        List<String> persistentDifficultyPatterns,
        GazeAnalysis gazeAnalysis,
        GazeTrend gazeTrend
) {
    public record Growth(LocalDate date, BigDecimal accuracy, BigDecimal readingSpeed,
                         BigDecimal pronunciationScore) {}
    public record AreaAchievement(String area, BigDecimal achievement) {}
    public record IncorrectWord(Long wordId, String wordName, int attemptCount,
                                int incorrectCount, BigDecimal incorrectRate) {}
    public record GazeAnalysis(
            Long gazeAnalysisResultId,
            Integer totalDwellTime,
            Integer dwellCount,
            Integer regressionCount,
            Integer averageFixationTime
    ) {
    }

    public record GazeTrend(
            LocalDateTime generatedAt,
            GazeSeries training,
            GazeSeries test
    ) {
    }

    public record GazeSeries(
            GazeSeriesStatus status,
            boolean comparisonAvailable,
            List<GazePoint> points,
            GazeChanges changes,
            List<String> descriptions,
            long failedSessionCount
    ) {
    }

    public enum GazeSeriesStatus {
        AVAILABLE,
        NO_DATA,
        FAILED
    }

    public record GazePoint(
            Long gazeAnalysisResultId,
            Long gazeSessionId,
            String sourceType,
            Long sourceId,
            LocalDateTime analyzedAt,
            Integer totalVisitedDurationMs,
            Integer totalVisitedCount,
            Integer reverseReadCount,
            Integer avgVisitedDurationMs
    ) {
    }

    public record GazeChanges(
            GazeMetricChange totalVisitedDurationMs,
            GazeMetricChange totalVisitedCount,
            GazeMetricChange reverseReadCount,
            GazeMetricChange avgVisitedDurationMs
    ) {
    }

    public record GazeMetricChange(
            Integer first,
            Integer latest,
            Integer delta
    ) {
    }
}
