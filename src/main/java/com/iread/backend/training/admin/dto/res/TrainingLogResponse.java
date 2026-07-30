package com.iread.backend.training.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingLogResponse(List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, String trainingName, LocalDateTime startedAt,
                               LocalDateTime endedAt, List<QuestionResult> questionResults,
                               BigDecimal accuracyRate, List<IncorrectItem> incorrectItems) {}
    public record QuestionResult(Integer questionNumber, Boolean isCorrect) {}
    public record IncorrectItem(Integer questionNumber, String question,
                                String correctAnswer, String selectedAnswer) {}
}
