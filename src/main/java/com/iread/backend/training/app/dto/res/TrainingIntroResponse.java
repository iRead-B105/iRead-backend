package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.generation.TrainingType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TrainingIntroResponse(
        Long trainingId,
        Long trainingTemplateId,
        TrainingType trainingType,
        Long dailyCurriculumId,
        Integer sequenceNo,
        TrainingStatus status,
        String trainingName,
        JsonNode generatedData,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
