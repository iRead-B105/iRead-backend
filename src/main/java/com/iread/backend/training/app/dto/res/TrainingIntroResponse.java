package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.TrainingStatus;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TrainingIntroResponse(
        Long trainingId,
        Long trainingTemplateId,
        Long dailyCurriculumId,
        Integer sequenceNo,
        TrainingStatus status,
        String trainingName,
        JsonNode generatedData,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
