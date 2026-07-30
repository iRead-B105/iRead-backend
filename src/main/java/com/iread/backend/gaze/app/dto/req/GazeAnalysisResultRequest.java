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
        @Schema(description = "학생 ID", example = "1")
        Long studentId,

        @PositiveOrZero
        @Schema(description = "총 시선 체류 시간(ms)", example = "65000")
        Integer totalVisitedDuration,

        @PositiveOrZero
        @Schema(description = "총 시선 체류 횟수", example = "23")
        Integer totalVisitedCount,

        @PositiveOrZero
        @Schema(description = "역행 횟수", example = "4")
        Integer reverseReadCount,

        @PositiveOrZero
        @Schema(description = "평균 응시 시간(ms)", example = "2826")
        Integer avgVisitedDuration,

        @Valid
        @Schema(description = "문장 또는 스토리 라인 단위 시선 분석 결과")
        List<SentenceMetric> sentenceMetrics,

        @Valid
        @Schema(description = "단어 단위 시선 분석 결과")
        List<WordAttempt> wordAttempts,

        @Valid
        @Schema(description = "역행 이벤트 목록")
        List<Regression> regressions,

        @Valid
        @Schema(description = "분석 메타데이터")
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
