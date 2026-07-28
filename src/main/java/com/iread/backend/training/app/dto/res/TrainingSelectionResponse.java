package com.iread.backend.training.app.dto.res;

import java.time.LocalDateTime;

public record TrainingSelectionResponse(
        Long attemptId,
        Long trainingId,
        Long wordId,
        Boolean isCorrect,
        Integer totalScore,
        LocalDateTime createdAt
) {
}
