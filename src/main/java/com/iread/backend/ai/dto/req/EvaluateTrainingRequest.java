package com.iread.backend.ai.dto.req;

import tools.jackson.databind.JsonNode;

import java.util.Objects;

public record EvaluateTrainingRequest(
        String requestId,
        Long trainingId,
        Long studentId,
        Long trainingTemplateId,
        int schemaVersion,
        JsonNode result
) {
    public EvaluateTrainingRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId는 필수입니다.");
        }
        requirePositive(trainingId, "trainingId");
        requirePositive(studentId, "studentId");
        requirePositive(trainingTemplateId, "trainingTemplateId");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion은 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(result, "result는 필수입니다.");
        if (!result.isObject()) {
            throw new IllegalArgumentException("result는 JSON 객체여야 합니다.");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(fieldName + "는 1 이상이어야 합니다.");
        }
    }
}
