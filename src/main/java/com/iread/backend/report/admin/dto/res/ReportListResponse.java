package com.iread.backend.report.admin.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportListResponse(
        Long reportId,
        Long studentId,
        String studentName,
        LocalDate startDate,
        LocalDate endDate,
        String teacherMemo,
        LocalDateTime createdAt
) {
}
