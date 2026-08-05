package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.generation.TrainingType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingIntroResponse(
        Long trainingId,
        Long trainingTemplateId,
        TrainingType trainingType,
        Long dailyCurriculumId,
        Integer sequenceNo,
        TrainingStatus status,
        String trainingName,
        JsonNode generatedData,
        List<Integer> completedQuestionNumbers,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
