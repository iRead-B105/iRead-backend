package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.time.LocalDateTime;

public record TestQuestionCompleteResponse(
        Long testId,
        int questionNumber,
        TestStatus status,
        LocalDateTime updatedAt
) {
}
