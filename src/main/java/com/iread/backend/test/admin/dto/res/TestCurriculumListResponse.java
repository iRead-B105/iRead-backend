package com.iread.backend.test.admin.dto.res;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TestCurriculumListResponse(List<Item> curriculums) {
    public TestCurriculumListResponse {
        curriculums = List.copyOf(curriculums);
    }

    public record Item(
            @JsonSerialize(using = ToStringSerializer.class)
            Long testCurriculumId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            int completedQuestions,
            int totalQuestions,
            BigDecimal overallScore
    ) {
    }
}
