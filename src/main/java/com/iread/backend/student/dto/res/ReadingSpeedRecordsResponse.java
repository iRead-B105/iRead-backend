package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReadingSpeedRecordsResponse(
        LocalDate from,
        LocalDate to,
        String unit,
        String calculationVersion,
        List<Record> records
) {
    public ReadingSpeedRecordsResponse {
        records = List.copyOf(records);
    }

    public record Record(
            String sourceType,
            Long sourceId,
            LocalDateTime measuredAt,
            long correctWordCount,
            long measuredDurationMs,
            BigDecimal readingSpeed,
            String unit,
            String calculationVersion
    ) {
    }
}
