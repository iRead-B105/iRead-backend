package com.iread.backend.ai.dto.req;

import tools.jackson.databind.JsonNode;

import java.util.Objects;

public record GenerateTrainingRequest(
        String requestId,
        Long trainingId,
        Long studentId,
        Long trainingTemplateId,
        int schemaVersion,
        JsonNode inputData
) {
    public GenerateTrainingRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId는 필수입니다.");
        }
        requirePositive(trainingId, "trainingId");
        requirePositive(studentId, "studentId");
        requirePositive(trainingTemplateId, "trainingTemplateId");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion은 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(inputData, "inputData는 필수입니다.");
        if (!inputData.isObject()) {
            throw new IllegalArgumentException("inputData는 JSON 객체여야 합니다.");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(fieldName + "는 1 이상이어야 합니다.");
        }
    }
}
