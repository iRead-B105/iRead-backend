package com.iread.backend.report.admin.dto.req;

import jakarta.validation.constraints.Size;

public record UpdateReportMemoRequest(
        @Size(max = 2000) String teacherMemo
) {
}
