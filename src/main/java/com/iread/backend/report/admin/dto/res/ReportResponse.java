package com.iread.backend.report.admin.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long studentId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        ReportSnapshot snapshot,
        String teacherMemo
) {
}
