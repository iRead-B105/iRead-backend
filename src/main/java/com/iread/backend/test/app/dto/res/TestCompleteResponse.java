package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TestCompleteResponse(
        Long testId,
        TestStatus status,
        BigDecimal accuracy,
        LocalDateTime completedAt
) {
}
