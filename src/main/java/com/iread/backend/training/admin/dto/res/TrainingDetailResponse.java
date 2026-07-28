package com.iread.backend.training.admin.dto.res;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrainingDetailResponse(
        Long trainingId,
        Long trainingTemplateId,
        String name,
        JsonNode form,
        JsonNode generatedData,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        JsonNode result,
        BigDecimal accuracy
) {
}
