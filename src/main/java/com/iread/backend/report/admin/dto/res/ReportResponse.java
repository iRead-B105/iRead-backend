package com.iread.backend.report.admin.dto.res;

import java.time.LocalDate;

public record ReportResponse(
        Long reportId,
        Long studentId,
        LocalDate startDate,
        LocalDate endDate,
        ReportSnapshot snapshot,
        String teacherMemo
) {}
