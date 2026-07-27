package com.iread.backend.report.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        GazeAnalysis gazeAnalysis
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
}
