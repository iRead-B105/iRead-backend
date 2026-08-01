package com.iread.backend.training.admin.dto.res;

import java.time.LocalDateTime;

public record CurriculumReviewResponse(
        Long curriculumId,
        String reviewStatus,
        Long reviewedByTeacherId,
        LocalDateTime reviewedAt
) {
}
