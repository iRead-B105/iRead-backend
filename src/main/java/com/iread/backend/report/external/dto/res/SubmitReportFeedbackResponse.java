package com.iread.backend.report.external.dto.res;

import java.time.LocalDateTime;

public record SubmitReportFeedbackResponse(
        Long feedbackId,
        LocalDateTime createdAt
) {
}
