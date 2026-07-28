package com.iread.backend.training.app.dto.res;

import java.time.LocalDateTime;

public record TrainingRecordingResponse(
        Long attemptId,
        Long trainingId,
        Long wordId,
        String recognizedText,
        String observedPronunciation,
        Double pronunciationScore,
        Double pronunciationConfidence,
        String pronunciationErrorType,
        Integer totalScore,
        LocalDateTime createdAt
) {
}
