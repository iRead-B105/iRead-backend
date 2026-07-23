package com.iread.backend.gaze.app.dto.req;

import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StartGazeSessionRequest(
        @NotNull
        @Schema(description = "학생 ID", example = "1")
        Long studentId,

        @NotNull
        @Schema(description = "콘텐츠 유형", example = "TEST")
        GazeContentType contentType,

        @Schema(description = "테스트 ID. contentType이 TEST일 때 사용합니다.", example = "1")
        Long testId,

        @Schema(description = "훈련 ID. contentType이 TRAINING일 때 사용합니다.", example = "1")
        Long trainingId,

        @Schema(description = "스토리 ID. contentType이 STORY일 때 사용합니다.", example = "1")
        Long storyId,

        @NotNull
        @Schema(description = "시선 보정 상태", example = "SUCCESS")
        GazeCalibrationStatus calibrationStatus
) {
}