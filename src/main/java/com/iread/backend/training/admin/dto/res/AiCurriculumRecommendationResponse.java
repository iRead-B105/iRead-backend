package com.iread.backend.training.admin.dto.res;

import java.util.List;

public record AiCurriculumRecommendationResponse(
        String recommendationProvider,
        String dataSufficiency,
        int currentStage,
        int maximumAllowedStage,
        String stageRationale,
        List<Recommendation> recommendations,
        List<String> warnings
) {
    public record Recommendation(
            int sequenceNo,
            Long trainingTemplateId,
            String trainingName,
            String role,
            int recommendedDifficulty,
            double score,
            List<String> targetFeatureCodes,
            List<String> reasonCodes,
            String rationale
    ) {
    }
}
