package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.time.LocalDateTime;

public record TestResetResponse(Long testId, TestStatus sessionState, LocalDateTime resetAt) {
}
