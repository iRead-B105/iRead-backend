package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AccuracyRecordsResponse(
        LocalDate from,
        LocalDate to,
        String unit,
        String calculationVersion,
        List<Record> records
) {
    public AccuracyRecordsResponse {
        records = List.copyOf(records);
    }

    public record Record(
            String sourceType,
            Long sourceId,
            LocalDateTime measuredAt,
            long correctAttemptCount,
            long attemptCount,
            BigDecimal accuracyRate,
            String unit,
            String calculationVersion
    ) {
    }
}
