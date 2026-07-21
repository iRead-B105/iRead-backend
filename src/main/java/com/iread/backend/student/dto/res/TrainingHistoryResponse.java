package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrainingHistoryResponse(
        LocalDate date,
        String learningType,
        Boolean learned,
        BigDecimal achievement
) {
}
