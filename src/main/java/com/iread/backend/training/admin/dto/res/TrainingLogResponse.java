package com.iread.backend.training.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingLogResponse(List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, String trainingName, LocalDateTime startedAt,
                               LocalDateTime finishedAt, BigDecimal accuracy, List<QuestionResult> questions) {}
    public record QuestionResult(Integer questionNumber, Long wordId, String question, Boolean correct,
                                 String correctAnswer, String selectedAnswer) {}
}
