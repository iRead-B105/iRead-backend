package com.iread.backend.gaze.app.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record GazeAnalysisDetailResponse(
        @Schema(description = "시선 트래킹 세션 ID", example = "1")
        Long gazeSessionId,

        @Schema(description = "시선 트래킹 분석 결과 ID", example = "1")
        Long gazeAnalysisResultId,

        @Schema(description = "총 시선 체류 시간(ms)", example = "65000")
        Integer totalVisitedDuration,

        @Schema(description = "총 시선 체류 횟수", example = "23")
        Integer totalVisitedCount,

        @Schema(description = "역행 횟수", example = "4")
        Integer reverseReadCount,

        @Schema(description = "평균 응시 시간(ms)", example = "2826")
        Integer avgVisitedDuration,

        @Schema(description = "문장 또는 스토리 라인 단위 분석 결과 JSON")
        String sentenceMetrics,

        @Schema(description = "역행 이벤트 목록 JSON")
        String regressions,

        @Schema(description = "분석 메타데이터 JSON")
        String analysisMeta
) {
}
