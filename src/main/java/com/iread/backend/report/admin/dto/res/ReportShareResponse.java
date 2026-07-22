package com.iread.backend.report.admin.dto.res;

import java.time.LocalDateTime;

public record ReportShareResponse(
        Long shareId,
        Long reportId,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        boolean expired
) {
}
