package com.iread.backend.report.external.dto.res;

import com.iread.backend.report.admin.dto.res.ReportSnapshot;

import java.time.LocalDate;

public record SharedReportResponse(
        String studentName,
        LocalDate startDate,
        LocalDate endDate,
        ReportSnapshot snapshot,
        String teacherMemo
) {
}
