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
        List<WordMetric> wordMetrics,
        JsonNode replay,
        AnalysisMeta analysisMeta
) {
    public StoryGazeAnalysisResponse {
        pageMetrics = List.copyOf(pageMetrics);
        wordMetrics = List.copyOf(wordMetrics);
        replay = replay == null ? null : replay.deepCopy();
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

    public record WordMetric(
            long storyLineId,
            int pageNo,
            int tokenIndex,
            String text,
            int dwellDurationMs,
            int visitCount,
            boolean skipped,
            int regressionCount,
            Integer firstSeenMs
    ) {
    }

    public record ReplayEvent(
            int pageNo,
            int eventIndex,
            int eventAtMs,
            Integer fromTokenIndex,
            int toTokenIndex,
            MovementType movementType,
            boolean dwellQualified,
            int dwellDurationMs,
            List<Integer> skippedTokenIndexes
    ) {
        public ReplayEvent {
            skippedTokenIndexes = List.copyOf(skippedTokenIndexes);
        }
    }

    public enum MovementType {
        READ,
        SKIP,
        REGRESSION
    }

    public record AnalysisMeta(
            String calculationVersion,
            String calculationSource,
            String heatmapScale,
            String dwellThresholdMethod,
            int sampleTailMs,
            int maxSampleGapMs,
            String firstSeenReference,
            boolean skipRequiresDwell,
            boolean regressionRequiresDwell
    ) {
        public static AnalysisMeta storyGazeWordV1() {
            return new AnalysisMeta(
                    "story-gaze-word-v1",
                    "BACKEND",
                    "PAGE_RELATIVE_MAX",
                    "PAGE_CHARACTER_AVERAGE",
                    80,
                    250,
                    "PAGE_FIRST_VALID_SAMPLE",
                    true,
                    true
            );
        }
    }
}
