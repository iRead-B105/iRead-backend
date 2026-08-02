package com.iread.backend.student.app.dto.res;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record LearningEntryResponse(
        Long studentId,
        LearningEntryStatus entryStatus,
        @JsonSerialize(using = ToStringSerializer.class)
        Long testCurriculumId,
        int completedQuestions,
        int totalQuestions
) {
}
