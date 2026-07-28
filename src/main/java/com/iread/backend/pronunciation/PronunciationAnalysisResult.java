package com.iread.backend.pronunciation;

public record PronunciationAnalysisResult(
        String requestId,
        String recognizedText,
        String expectedPronunciation,
        String observedPronunciation,
        double pronunciationScore,
        double confidence,
        String errorType,
        String analysisVersion
) {
}
