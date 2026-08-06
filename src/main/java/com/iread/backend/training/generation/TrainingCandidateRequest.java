package com.iread.backend.training.generation;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record TrainingCandidateRequest(
        String requestId,
        int schemaVersion,
        TrainingType trainingType,
        int count,
        int difficulty,
        List<TrainingTargetFeature> targetFeatures,
        List<String> excludedFeatures,
        String additionalPrompt,
        JsonNode outputTemplate
) {
    public TrainingCandidateRequest {
        if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
            throw new IllegalArgumentException("requestId는 1~128자여야 합니다.");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion은 1 이상이어야 합니다.");
        }
        if (trainingType == null) {
            throw new IllegalArgumentException("trainingType은 필수입니다.");
        }
        if (count < 1 || count > 9) {
            throw new IllegalArgumentException("count는 1~9여야 합니다.");
        }
        if (difficulty < 1 || difficulty > 5) {
            throw new IllegalArgumentException("difficulty는 1~5여야 합니다.");
        }
        targetFeatures = targetFeatures == null ? List.of() : List.copyOf(targetFeatures);
        if (targetFeatures.size() > 2) {
            throw new IllegalArgumentException("targetFeatures는 최대 2개입니다.");
        }
        excludedFeatures = excludedFeatures == null ? List.of() : List.copyOf(excludedFeatures);
        if (excludedFeatures.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("excludedFeatures에는 빈 코드를 넣을 수 없습니다.");
        }
        if (excludedFeatures.stream().distinct().count() != excludedFeatures.size()) {
            throw new IllegalArgumentException("excludedFeatures는 중복할 수 없습니다.");
        }
        additionalPrompt = additionalPrompt == null ? "" : additionalPrompt;
        if (outputTemplate == null || !outputTemplate.isObject()) {
            throw new IllegalArgumentException("outputTemplate은 JSON 객체여야 합니다.");
        }
        outputTemplate = outputTemplate.deepCopy();
    }
}
