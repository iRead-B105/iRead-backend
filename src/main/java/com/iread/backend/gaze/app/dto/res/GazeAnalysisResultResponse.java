package com.iread.backend.gaze.app.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GazeAnalysisResultResponse(
        @Schema(description = "시선 트래킹 분석 ID", example = "1")
        Long gazeAnalysisId,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {
}
