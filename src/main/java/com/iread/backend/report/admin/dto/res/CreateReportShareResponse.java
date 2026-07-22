package com.iread.backend.report.admin.dto.res;

import java.time.LocalDateTime;

public record CreateReportShareResponse(
        Long shareId,
        String shareUrl,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
