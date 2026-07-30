package com.iread.backend.student.app.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "growth.stage")
public record GrowthStageProperties(
        int recentWindowSize,
        int masteryAccuracy,
        int sproutCompleted,
        int sproutDistinctTemplates,
        int budCompleted,
        int budCoveragePercent,
        int flowerCompleted,
        int flowerCoveragePercent,
        int flowerRecentAccuracy,
        int fullBloomCompleted,
        int fullBloomMasteryCoveragePercent,
        int fullBloomRecentAccuracy
) {
    public GrowthStageProperties {
        requirePositive(recentWindowSize, "recent-window-size");
        requirePercent(masteryAccuracy, "mastery-accuracy");
        requireNonNegative(sproutCompleted, "sprout-completed");
        requireNonNegative(sproutDistinctTemplates, "sprout-distinct-templates");
        requireNonNegative(budCompleted, "bud-completed");
        requirePercent(budCoveragePercent, "bud-coverage-percent");
        requireNonNegative(flowerCompleted, "flower-completed");
        requirePercent(flowerCoveragePercent, "flower-coverage-percent");
        requirePercent(flowerRecentAccuracy, "flower-recent-accuracy");
        requireNonNegative(fullBloomCompleted, "full-bloom-completed");
        requirePercent(
                fullBloomMasteryCoveragePercent,
                "full-bloom-mastery-coverage-percent"
        );
        requirePercent(fullBloomRecentAccuracy, "full-bloom-recent-accuracy");
        if (sproutCompleted > budCompleted
                || budCompleted > flowerCompleted
                || flowerCompleted > fullBloomCompleted) {
            throw new IllegalArgumentException(
                    "growth.stage 완료 횟수 임계값은 단계가 높아질수록 작아질 수 없습니다."
            );
        }
        if (budCoveragePercent > flowerCoveragePercent
                || flowerCoveragePercent > fullBloomMasteryCoveragePercent) {
            throw new IllegalArgumentException(
                    "growth.stage 범위 임계값은 단계가 높아질수록 작아질 수 없습니다."
            );
        }
        if (flowerRecentAccuracy > fullBloomRecentAccuracy) {
            throw new IllegalArgumentException(
                    "growth.stage 최근 정확도 임계값은 단계가 높아질수록 작아질 수 없습니다."
            );
        }
    }

    private static void requirePositive(int value, String property) {
        if (value <= 0) {
            throw new IllegalArgumentException("growth.stage." + property + "는 0보다 커야 합니다.");
        }
    }

    private static void requireNonNegative(int value, String property) {
        if (value < 0) {
            throw new IllegalArgumentException("growth.stage." + property + "는 0 이상이어야 합니다.");
        }
    }

    private static void requirePercent(int value, String property) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("growth.stage." + property + "는 0~100이어야 합니다.");
        }
    }
}
