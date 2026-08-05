package com.iread.backend.training.admin.dto.res;

import java.time.OffsetDateTime;
import java.util.List;

public record SaveLessonMaterialResponse(
        Long trainingId,
        int revision,
        OffsetDateTime savedAt,
        String source,
        List<LessonMaterialResponse.Material> materials
) {
}
