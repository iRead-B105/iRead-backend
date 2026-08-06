package com.iread.backend.report.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record CreateReportRequest(
        @NotNull @Positive Long studentId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
    // 미래 날짜 제한은 아동의 학습 날짜 기준으로 ReportService 에서 확인한다.
    // 데모 치트로 학습일을 넘기면 학습 날짜가 달력상 오늘보다 앞서므로
    // @PastOrPresent 로는 그날 기록을 보고서에 담을 수 없다.
    @AssertTrue(message = "종료일은 시작일과 같거나 이후여야 합니다.")
    public boolean isPeriodOrdered() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
