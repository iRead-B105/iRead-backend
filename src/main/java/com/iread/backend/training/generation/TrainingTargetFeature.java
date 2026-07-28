package com.iread.backend.training.generation;

public record TrainingTargetFeature(
        String featureCode,
        double weaknessScore,
        double confidence,
        int evidenceCount
) {
    public TrainingTargetFeature {
        if (featureCode == null || featureCode.isBlank()) {
            throw new IllegalArgumentException("featureCode는 필수입니다.");
        }
        requireUnitRange(weaknessScore, "weaknessScore");
        requireUnitRange(confidence, "confidence");
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount는 0 이상이어야 합니다.");
        }
    }

    private static void requireUnitRange(double value, String field) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(field + "는 0 이상 1 이하여야 합니다.");
        }
    }
}
