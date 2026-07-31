package com.iread.backend.training.generation;

import com.iread.backend.training.domain.TrainingTemplateEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class TrainingTemplateContract {

    private TrainingTemplateContract() {
    }

    public static TrainingType trainingType(
            TrainingTemplateEntity template,
            ObjectMapper objectMapper
    ) {
        JsonNode prompt;
        try {
            prompt = objectMapper.readTree(template.getPrompt());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "훈련 템플릿 prompt JSON을 읽을 수 없습니다: " + template.getId(),
                    exception
            );
        }
        try {
            return TrainingType.from(prompt.path("trainingType").asText());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "훈련 템플릿의 trainingType을 읽을 수 없습니다: " + template.getId(),
                    exception
            );
        }
    }
}
