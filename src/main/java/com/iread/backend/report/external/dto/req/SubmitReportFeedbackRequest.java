package com.iread.backend.report.external.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitReportFeedbackRequest(
        @NotBlank
        @Size(max = 2000)
        String content
) {
}
