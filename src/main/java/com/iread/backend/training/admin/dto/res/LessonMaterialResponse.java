package com.iread.backend.training.admin.dto.res;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record LessonMaterialResponse(
        Long trainingId,
        Long trainingTemplateId,
        String trainingName,
        String unitName,
        String status,
        int schemaVersion,
        int revision,
        boolean editable,
        List<Material> materials
) {
    public record Material(
            int questionNo,
            String questionType,
            String responseType,
            List<String> requiredInputs,
            JsonNode presentation,
            JsonNode content,
            JsonNode answer
    ) {
    }
}
