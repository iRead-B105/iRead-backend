package com.iread.backend.ai.dto.res;

import java.math.BigDecimal;

public record EvaluateTrainingResponse(
        String requestId,
        int schemaVersion,
        BigDecimal accuracy
) {
}
