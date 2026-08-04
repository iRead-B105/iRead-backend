package com.iread.backend.training.admin.dto.res;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingLogResponse(List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, String trainingName, LocalDateTime startedAt,
                               LocalDateTime endedAt, List<QuestionResult> questionResults,
                               BigDecimal accuracyRate, List<IncorrectItem> incorrectItems,
                               List<TrainingQuestionResult> questions) {}
    public record QuestionResult(Integer questionNumber, Boolean isCorrect) {}
    public record IncorrectItem(Integer questionNumber, String question,
                                String correctAnswer, String selectedAnswer) {}
    public record TrainingQuestionResult(
            Integer questionNo,
            String questionType,
            JsonNode question,
            String responseType,
            JsonNode selectedAnswer,
            JsonNode correctAnswer,
            Boolean correct,
            BigDecimal score
    ) {}
}
