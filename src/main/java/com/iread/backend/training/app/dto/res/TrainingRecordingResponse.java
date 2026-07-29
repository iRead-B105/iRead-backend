package com.iread.backend.training.app.dto.res;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingRecordingResponse(
        Long trainingId,
        Integer questionNumber,
        Double pronunciationAccuracyScore,
        Double fluencyScore,
        Double completenessScore,
        Double pronScore,
        Double pronunciationConfidence,
        String analysisVersion,
        List<WordResult> words,
        LocalDateTime createdAt
) {
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
