package com.iread.backend.test.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TestCurriculumListResponse(List<Item> curriculums) {
    public TestCurriculumListResponse {
        curriculums = List.copyOf(curriculums);
    }

    public record Item(
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
