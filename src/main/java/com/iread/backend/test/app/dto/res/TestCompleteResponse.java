package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.time.LocalDateTime;

public record TestCompleteResponse(
        String completionType,
        Long testId,
        TestStatus status,
        LocalDateTime completedAt,
        String messageKey,
        String nextAction
) {
}
