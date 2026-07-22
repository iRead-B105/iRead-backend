package com.iread.backend.report.admin.dto.res;

import java.time.LocalDateTime;

public record ReportFeedbackResponse(
        Long feedbackId,
        Long reportId,
        Long studentId,
        String studentName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
