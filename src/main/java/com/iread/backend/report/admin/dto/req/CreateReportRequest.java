package com.iread.backend.report.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record CreateReportRequest(
        @NotNull @Positive Long studentId,
        @NotNull @PastOrPresent LocalDate startDate,
        @NotNull @PastOrPresent LocalDate endDate
) {
    @AssertTrue(message = "종료일은 시작일과 같거나 이후여야 합니다.")
    public boolean isPeriodOrdered() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
