package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingHistoryResponse(
        LocalDate date,
        String learningType,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        BigDecimal achievement
) {
}
