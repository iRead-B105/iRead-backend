package com.iread.backend.student.app.dto.res;

public record TrainingProgressResponse(
        Long trainingTemplateId,
        String trainingTemplateName,
        Long completedCount
) {
}
