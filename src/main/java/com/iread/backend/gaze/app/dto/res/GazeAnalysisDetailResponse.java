package com.iread.backend.gaze.app.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record GazeAnalysisDetailResponse(
        @Schema(description = "Gaze session ID", example = "1")
        Long gazeSessionId,

        @Schema(description = "Gaze analysis result ID", example = "1")
        Long gazeAnalysisResultId,

        @Schema(description = "Total gaze dwell duration in milliseconds", example = "65000")
        Integer totalVisitedDuration,

        @Schema(description = "Total gaze fixation count", example = "23")
        Integer totalVisitedCount,

        @Schema(description = "Regression count", example = "4")
        Integer reverseReadCount,

        @Schema(description = "Average fixation duration in milliseconds", example = "2826")
        Integer avgVisitedDuration,

        @Schema(description = "Sentence or story-line level metrics as JSON")
        String sentenceMetrics,

        @Schema(description = "Regression events as JSON")
        String regressions,

        @Schema(description = "Analysis metadata as JSON")
        String analysisMeta
) {
}
