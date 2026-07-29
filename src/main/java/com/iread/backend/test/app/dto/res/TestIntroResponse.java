package com.iread.backend.test.app.dto.res;

import com.iread.backend.test.domain.TestStatus;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record TestIntroResponse(
        Long testId,
        Long studentId,
        Long trainingTemplateId,
        Integer sequenceNo,
        String trainingName,
        JsonNode generatedData,
        LocalDateTime createdAt,
        TestStatus status,
        int totalQuestions
) {
}
