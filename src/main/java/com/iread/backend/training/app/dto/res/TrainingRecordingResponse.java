package com.iread.backend.training.app.dto.res;

import java.time.LocalDateTime;

public record TrainingRecordingResponse(
        Long attemptId,
        Long trainingId,
        Long wordId,
        String recognizedText,
        Integer totalScore,
        LocalDateTime createdAt
) {
}
