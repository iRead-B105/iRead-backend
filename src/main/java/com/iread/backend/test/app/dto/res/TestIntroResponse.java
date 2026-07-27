package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.time.LocalDateTime;

public record TestIntroResponse(
        Long testId,
        Long studentId,
        LocalDateTime createdAt,
        TestStatus status,
        int totalQuestions
) {
}
