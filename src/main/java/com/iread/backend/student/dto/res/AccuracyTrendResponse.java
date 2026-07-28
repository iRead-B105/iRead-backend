package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccuracyTrendResponse(
        LocalDate date,
        BigDecimal accuracyRate
) {
}
