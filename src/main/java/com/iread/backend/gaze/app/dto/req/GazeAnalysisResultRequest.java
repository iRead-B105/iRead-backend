package com.iread.backend.gaze.app.dto.req;

import com.iread.backend.gaze.domain.GazeContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record GazeAnalysisResultRequest(

        @NotNull
        @Schema(description = "Student ID", example = "1")
        Long studentId,

        @PositiveOrZero
        @Schema(description = "Total gaze dwell duration in milliseconds", example = "65000")
        Integer totalVisitedDuration,

        @PositiveOrZero
        @Schema(description = "Total gaze fixation count", example = "23")
        Integer totalVisitedCount,

        @PositiveOrZero
        @Schema(description = "Regression count", example = "4")
        Integer reverseReadCount,

        @PositiveOrZero
        @Schema(description = "Average fixation duration in milliseconds", example = "2826")
        Integer avgVisitedDuration,

        @Valid
        @Schema(description = "Sentence or story-line level gaze metrics")
        List<SentenceMetric> sentenceMetrics,

        @Valid
        @Schema(description = "Word level gaze analysis results")
        List<WordAttempt> wordAttempts,

        @Valid
        @Schema(description = "Regression events")
        List<Regression> regressions,

        @Valid
        @Schema(description = "Analysis metadata")
        AnalysisMeta analysisMeta
) {
    public record SentenceMetric(
            Long storyLineId,
            Integer sequenceNo,
            String surfaceText,
            @PositiveOrZero Integer dwellDurationMs,
            @PositiveOrZero Integer fixationCount,
            @PositiveOrZero Integer firstGazeOffsetMs,
            @PositiveOrZero Integer lastGazeOffsetMs
    ) {
    }

    public record WordAttempt(
            Long wordId,
            Long storyLineId,
            Long trainingId,
            Long testId,
            @NotNull Integer wordIndex,
            @NotBlank String surfaceText,
            Boolean hasAudioData,
            @PositiveOrZero Integer fixationDurationMs,
            @PositiveOrZero Integer fixationCount,
            @PositiveOrZero Integer gazeStartOffsetMs,
            @PositiveOrZero Integer gazeEndOffsetMs,
            Boolean isRead,
            Boolean isFixated,
            Boolean isSkipped,
            Boolean isRegressed,
            @PositiveOrZero Integer regressionCount
    ) {
    }

    public record Regression(
            @NotNull Integer fromWordIndex,
            @NotNull Integer toWordIndex,
            @PositiveOrZero Integer offsetMs
    ) {
    }

    public record AnalysisMeta(
            GazeContentType contentType,
            Long storyId,
            Long trainingId,
            Long testId,
            String calculationSource,
            @PositiveOrZero Integer gazeSessionDurationMs
    ) {
    }
}
