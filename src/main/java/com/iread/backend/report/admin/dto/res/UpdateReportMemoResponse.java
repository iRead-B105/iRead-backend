package com.iread.backend.report.admin.dto.res;

import java.time.LocalDateTime;

public record UpdateReportMemoResponse(
        Long reportId,
        String teacherMemo,
        LocalDateTime createdAt
) {
}
