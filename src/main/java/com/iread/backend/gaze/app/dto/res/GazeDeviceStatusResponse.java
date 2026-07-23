package com.iread.backend.gaze.app.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record GazeDeviceStatusResponse(
        @Schema(description = "연결 여부", example = "true")
        boolean connected,

        @Schema(description = "장치명", example = "Web Eye Tracker")
        String deviceName,

        @Schema(description = "장치 상태", example = "READY")
        String deviceStatus,

        @Schema(description = "안내 메시지", example = "시선 추적 장치를 사용할 수 있습니다.")
        String guideMessage
) {
}