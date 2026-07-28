package com.iread.backend.pronunciation;

public record PronunciationAnalysisResult(
        String requestId,
        double pronunciationAccuracyScore,
        double confidence,
        String errorType,
        String analysisVersion
) {
}
