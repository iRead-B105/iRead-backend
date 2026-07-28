package com.iread.backend.student.app.dto.res;

import java.util.List;

public record GrowthResponse(
        List<TrainingProgressResponse> trainingProgress
) {
}
