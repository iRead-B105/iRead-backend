package com.iread.backend.report.admin.dto.req;

import com.iread.backend.validation.SafeText;
import jakarta.validation.constraints.Size;

public record UpdateReportMemoRequest(
        @Size(max = 2000) @SafeText(allowLineBreaks = true) String teacherMemo
) {
}
