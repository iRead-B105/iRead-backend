package com.iread.backend.test.app.dto.res;

import java.time.LocalDateTime;
import java.util.List;

public record TestRecordingResponse(
        Long testId,
        Integer questionNumber,
        Double pronunciationAccuracyScore,
        Double fluencyScore,
        Double completenessScore,
        Double pronScore,
        Double pronunciationConfidence,
        String analysisVersion,
        int pronunciationThreshold,
        int attemptNo,
        int maxAttempts,
        boolean passed,
        boolean questionCompleted,
        boolean canRetry,
        List<WordResult> words,
        LocalDateTime createdAt
) {
    public String pronunciationErrorType() {
        return words.isEmpty() ? null : words.getFirst().pronunciationErrorType();
    }

    public Integer totalScore() {
        return words.isEmpty() ? null : words.getFirst().totalScore();
    }

    public record WordResult(
            Long attemptId,
            Long wordId,
            Integer tokenIndex,
            String surfaceText,
            Double pronunciationAccuracyScore,
            String pronunciationErrorType,
            Integer totalScore,
            Integer speechStartOffsetMs,
            Integer speechEndOffsetMs,
            LocalDateTime createdAt
    ) {
    }
}
