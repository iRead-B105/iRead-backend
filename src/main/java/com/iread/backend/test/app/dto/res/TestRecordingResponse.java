package com.iread.backend.test.app.dto.res;

import java.time.LocalDateTime;

public record TestRecordingResponse(
        Long attemptId,
        Long testId,
        Long wordId,
        Double pronunciationAccuracyScore,
        String pronunciationErrorType,
        Integer totalScore,
        LocalDateTime createdAt
) {
}
