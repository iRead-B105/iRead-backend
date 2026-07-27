package com.iread.backend.test.app.dto.res;

import java.time.LocalDateTime;

public record TestSelectionResponse(
        Long attemptId,
        Long testId,
        Long wordId,
        Boolean isCorrect,
        Integer totalScore,
        LocalDateTime createdAt
) {
}
