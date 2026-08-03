package com.iread.backend.story.admin.dto.res;

import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;

public record StoryGazeAnalysisResponse(
        Long gazeSessionId,
        Long gazeAnalysisId,
        GazeCalibrationStatus calibrationStatus,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer totalDwellTime,
        Integer dwellCount,
        Integer regressionCount,
        Integer averageFixationTime,
        List<PageMetric> pageMetrics,
        JsonNode analysisMeta
) {
    public StoryGazeAnalysisResponse {
        pageMetrics = List.copyOf(pageMetrics);
        analysisMeta = analysisMeta == null ? null : analysisMeta.deepCopy();
    }

    public record PageMetric(
            Long storyLineId,
            int pageNo,
            String surfaceText,
            int dwellDurationMs,
            int fixationCount,
            int regressionCount,
            Integer averageFixationTimeMs,
            int firstGazeOffsetMs,
            int lastGazeOffsetMs,
            List<Regression> regressions
    ) {
        public PageMetric {
            regressions = List.copyOf(regressions);
        }
    }

    public record Regression(
            int fromTokenIndex,
            int toTokenIndex,
            int offsetMs
    ) {
    }
}
