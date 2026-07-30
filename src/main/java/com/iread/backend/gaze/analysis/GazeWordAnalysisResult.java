package com.iread.backend.gaze.analysis;

public record GazeWordAnalysisResult(
        String requestId,
        int fixationDurationMs,
        int fixationCount,
        int regressionCount,
        boolean skipped,
        double confidence,
        String analysisVersion
) {
}
