package com.iread.backend.gaze.app.dto.req;

import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record EndGazeSessionRequest(
        @NotNull
        @Schema(description = "학생 ID", example = "1")
        Long studentId,

        @NotNull
        @JsonProperty("endStatus")
        @Schema(description = "종료 상태. COMPLETED 또는 FAILED만 사용할 수 있습니다.", example = "COMPLETED")
        GazeSessionStatus endStatus,

        @Schema(description = "초당 5~10프레임으로 수집한 원시 시선 데이터")
        JsonNode data
) {
}
