package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingHistoryResponse(
        Long trainingId,
        LocalDate date,
        String learningType,
        String learningCategory,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        BigDecimal accuracyRate,
        List<QuestionResult> questions
) {
    public record QuestionResult(
            Integer questionNumber,
            String question,
            Boolean correct,
            String selectedAnswer,
            String correctAnswer
    ) {
    }
}
