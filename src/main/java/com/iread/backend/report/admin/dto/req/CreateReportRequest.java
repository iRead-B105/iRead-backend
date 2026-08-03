package com.iread.backend.report.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateReportRequest(
        @NotNull Long studentId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}
