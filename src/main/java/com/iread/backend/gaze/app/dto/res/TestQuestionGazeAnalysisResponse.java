package com.iread.backend.gaze.app.dto.res;

import java.util.List;

public record TestQuestionGazeAnalysisResponse(
        Long testId,
        int questionNo,
        Long gazeSessionId,
        Long gazeAnalysisId,
        int totalDwellTime,
        int dwellCount,
        int regressionCount,
        Integer averageFixationTime,
        List<WordMetric> wordMetrics,
        AnalysisMeta analysisMeta
) {
    public TestQuestionGazeAnalysisResponse {
        wordMetrics = List.copyOf(wordMetrics);
    }

    public record WordMetric(
            Integer targetIndex,
            Integer tokenIndex,
            String text,
            int dwellDurationMs,
            int visitCount,
            Boolean skipped,
            int regressionCount,
            Integer firstSeenMs,
            Integer lastSeenMs
    ) {
    }

    public record AnalysisMeta(
            String calculationVersion,
            String calculationSource
    ) {
    }
}
