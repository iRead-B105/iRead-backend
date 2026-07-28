package com.iread.backend.training.generation;

import tools.jackson.databind.JsonNode;

public record TrainingCandidateResponse(
        String type,
        JsonNode data
) {
    public TrainingCandidateResponse {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("AI 후보 응답 type은 필수입니다.");
        }
        if (data == null || !data.isArray()) {
            throw new IllegalArgumentException("AI 후보 응답 data는 배열이어야 합니다.");
        }
        data = data.deepCopy();
    }
}
