package com.iread.backend.student.dto.res;

import java.util.List;

public record TrainingHistoryDataResponse(
        List<TrainingHistoryResponse> learningHistory
) {
}
