package com.iread.backend.gaze.app.dto.res;

import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GazeSessionResponse(
        @Schema(description = "시선 트래킹 세션 ID", example = "1")
        Long gazeSessionId,

        @Schema(description = "콘텐츠 유형", example = "TEST")
        GazeContentType contentType,

        @Schema(description = "수집 상태", example = "RUNNING")
        GazeSessionStatus status,

        @Schema(description = "시선 보정 상태", example = "SUCCESS")
        GazeCalibrationStatus calibrationStatus,

        @Schema(description = "수집 시작 시간")
        LocalDateTime startedAt,

        @Schema(description = "수집 종료 시간")
        LocalDateTime endedAt
) {
}