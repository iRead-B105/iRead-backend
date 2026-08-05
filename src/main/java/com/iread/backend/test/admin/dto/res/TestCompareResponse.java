package com.iread.backend.test.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TestCompareResponse(TestDetail currentTest, List<TestDetail> comparisonTests) {
    public record TestDetail(
            Long testId,
            LocalDate date,
            BigDecimal overallScore,
            BigDecimal changeFromPrevious,
            List<String> strengthAreas,
            List<String> improvementAreas,
            String recommendedCourse,
            String nextTestRecommendation,
            List<AreaScore> areaScores,
            Long readingTimeSeconds,
            Long solvingTimeSeconds,
            BigDecimal accuracy,
            Integer gazeDepartureCount,
            List<QuestionResult> questions
    ) {}

    public record AreaScore(String area, BigDecimal score) {}

    public record QuestionResult(Integer questionNumber, String question, Boolean isCorrect,
                                 String correctAnswer, String selectedAnswer) {}
}
