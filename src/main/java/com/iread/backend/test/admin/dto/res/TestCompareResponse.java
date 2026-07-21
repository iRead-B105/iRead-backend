package com.iread.backend.test.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TestCompareResponse(TestDetail currentTest, List<TestDetail> comparisonTests) {
    public record TestDetail(Long testId, LocalDate date, Long readingTimeSeconds, Long solvingTimeSeconds,
                             BigDecimal accuracy, Integer gazeDepartureCount, List<QuestionResult> questions) {}

    public record QuestionResult(Integer questionNumber, String question, Boolean isCorrect,
                                 String correctAnswer, String selectedAnswer) {}
}
