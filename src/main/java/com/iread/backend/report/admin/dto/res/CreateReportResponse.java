package com.iread.backend.report.admin.dto.res;

import java.time.LocalDateTime;

public record CreateReportResponse(
        Long reportId,
        LocalDateTime createdAt
) {
}
