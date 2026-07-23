package com.iread.backend.gaze.app.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record GazeCalibrationGuideResponse(
        @Schema(description = "보정 필요 여부", example = "true")
        boolean calibrationRequired,

        @Schema(description = "보정 안내 문구", example = "화면 중앙의 점을 바라보며 보정을 진행해 주세요.")
        String guideMessage
) {
}