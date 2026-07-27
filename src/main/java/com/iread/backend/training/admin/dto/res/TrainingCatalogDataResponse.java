package com.iread.backend.training.admin.dto.res;

import java.util.List;

public record TrainingCatalogDataResponse(
        List<TrainingCatalogResponse> trainingTypes
) {
}
