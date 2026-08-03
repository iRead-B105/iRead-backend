package com.iread.backend.test.admin.dto.res;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TestCurriculumDetailResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long testCurriculumId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        int completedQuestions,
        int totalQuestions,
        BigDecimal overallScore,
        List<AreaScore> areaScores,
        Long solvingTimeSeconds,
        List<QuestionResult> questions,
        String recommendationStatus,
        String recommendationError,
        LocalDateTime recommendationLastAttemptAt,
        int recommendationRetryCount,
        Long dailyCurriculumId,
        String contentGenerationStatus,
        String teacherReviewStatus
) {
    public TestCurriculumDetailResponse {
        areaScores = List.copyOf(areaScores);
        questions = List.copyOf(questions);
    }

    public record AreaScore(
            String trackCode,
            String title,
            BigDecimal score,
            int completedQuestions,
            int totalQuestions
    ) {
    }

    public record QuestionResult(
            @JsonSerialize(using = ToStringSerializer.class) Long testId,
            int sequenceNo,
            String trackCode,
            String questionType,
            String question,
            String responseType,
            JsonNode selectedAnswer,
            JsonNode correctAnswer,
            Boolean correct,
            BigDecimal score,
            BigDecimal pronunciationScore,
            Long solvingTimeSeconds,
            Integer gazeDepartureCount
    ) {
    }
}
