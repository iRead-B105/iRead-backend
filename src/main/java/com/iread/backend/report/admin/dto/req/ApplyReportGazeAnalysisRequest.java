package com.iread.backend.report.admin.dto.req;

import jakarta.validation.constraints.NotNull;

public record ApplyReportGazeAnalysisRequest(
        @NotNull Long gazeAnalysisResultId
) {
}
