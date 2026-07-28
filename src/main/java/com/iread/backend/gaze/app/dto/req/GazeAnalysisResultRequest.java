package com.iread.backend.gaze.app.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

public record GazeAnalysisResultRequest(

        @NotNull
        @Schema(description = "학생 ID", example = "1")
        Long studentId,

        @NotNull
        @PositiveOrZero
        @Schema(description = "총 시선 체류 시간(ms)", example = "65000")
        Integer totalVisitedDuration,

        @NotNull
        @PositiveOrZero
        @Schema(description = "시선 체류 횟수", example = "23")
        Integer totalVisitedCount,

        @NotNull
        @PositiveOrZero
        @Schema(description = "시선 역행(되돌아보기) 횟수", example = "4")
        Integer reverseReadCount,

        @PositiveOrZero
        @Schema(description = "평균 응시 시간(ms)", example = "2826")
        Integer avgVisitedDuration,

        @Schema(description = "이야기 문장별 시선 분석 지표")
        JsonNode sentenceMetrics
) {
}
