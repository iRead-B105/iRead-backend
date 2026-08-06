package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.app.dto.res.GrowthAreaResponse;
import com.iread.backend.student.app.dto.res.GrowthResponse;
import com.iread.backend.student.app.dto.res.TrainingProgressResponse;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrowthService {
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final GrowthStageProperties stageProperties;

    public GrowthResponse getGrowth(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        List<TrainingProgressResponse> trainingProgress = trainingRepository
                .findCompletedTrainingProgress(studentId)
                .stream()
                .map(progress -> new TrainingProgressResponse(
                        progress.getTrainingTemplateId(),
                        progress.getTrainingTemplateName(),
                        progress.getCompletedCount()
                ))
                .toList();
        List<TrainingEntity> completedTrainings = new ArrayList<>(
                trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(
                        studentId,
                        TrainingStatus.COMPLETED
                )
        );
        completedTrainings.sort(Comparator.comparing(
                TrainingEntity::getFinishedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        EnumMap<GrowthArea, GrowthAccumulator> accumulators =
                new EnumMap<>(GrowthArea.class);
        for (GrowthArea area : GrowthArea.values()) {
            accumulators.put(area, new GrowthAccumulator(area, stageProperties));
        }
        for (TrainingEntity training : completedTrainings) {
            GrowthArea.fromTemplateId(training.getTrainingTemplate().getId())
                    .ifPresent(area -> accumulators.get(area).accept(training));
        }

        // 훈련을 완료한 서로 다른 날짜 수. 완료 훈련 개수와 달라야 한다.
        // 하루에 여러 훈련을 끝내도 하루로 센다.
        long studyDayCount = completedTrainings.stream()
                .map(TrainingEntity::getFinishedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();

        return new GrowthResponse(
                studyDayCount,
                trainingProgress,
                List.of(GrowthArea.values()).stream()
                        .map(area -> accumulators.get(area).response())
                        .toList()
        );
    }

    /**
     * 템플릿 ID 범위는 학습자 앱의 세 성장 화단 계약과 같다.
     * 새 템플릿 분류를 추가할 때는 OpenAPI 성장 영역 설명과 Frontend 매핑을 함께 변경한다.
     */
    private enum GrowthArea {
        PHONICS(1, "파닉스", 1, 21),
        READING(2, "읽기", 22, 29),
        FLUENCY(3, "유창성", 30, 34);

        private final int id;
        private final String displayName;
        private final long firstTemplateId;
        private final long lastTemplateId;

        GrowthArea(
                int id,
                String displayName,
                long firstTemplateId,
                long lastTemplateId
        ) {
            this.id = id;
            this.displayName = displayName;
            this.firstTemplateId = firstTemplateId;
            this.lastTemplateId = lastTemplateId;
        }

        int totalTemplateCount() {
            // 커버리지 분모는 실제로 편성될 수 있는 템플릿 수여야 한다.
            // 은퇴 템플릿(통합·제거된 ID)을 분모에 넣으면 만점 커버리지가 불가능해진다.
            return TrainingCatalogPolicy.selectableCountInRange(firstTemplateId, lastTemplateId);
        }

        static java.util.Optional<GrowthArea> fromTemplateId(Long templateId) {
            if (templateId == null) {
                return java.util.Optional.empty();
            }
            for (GrowthArea area : values()) {
                if (templateId >= area.firstTemplateId
                        && templateId <= area.lastTemplateId) {
                    return java.util.Optional.of(area);
                }
            }
            return java.util.Optional.empty();
        }
    }

    private static final class GrowthAccumulator {
        private static final List<String> STAGE_NAMES =
                List.of("흙", "새싹", "꽃봉오리", "꽃", "만개");

        private final GrowthArea area;
        private final GrowthStageProperties properties;
        private final Set<Long> experiencedTemplates = new HashSet<>();
        private final Set<Long> masteredTemplates = new HashSet<>();
        private final ArrayDeque<BigDecimal> recentAccuracies = new ArrayDeque<>();
        private long completedCount;
        private int highestStage = 1;
        private LocalDateTime updatedAt;

        private GrowthAccumulator(
                GrowthArea area,
                GrowthStageProperties properties
        ) {
            this.area = area;
            this.properties = properties;
        }

        private void accept(TrainingEntity training) {
            completedCount++;
            Long templateId = training.getTrainingTemplate().getId();
            experiencedTemplates.add(templateId);
            BigDecimal accuracy = training.getAccuracy();
            if (accuracy != null) {
                recentAccuracies.addLast(accuracy);
                while (recentAccuracies.size() > properties.recentWindowSize()) {
                    recentAccuracies.removeFirst();
                }
                if (accuracy.compareTo(
                        BigDecimal.valueOf(properties.masteryAccuracy())
                ) >= 0) {
                    masteredTemplates.add(templateId);
                }
            }
            if (training.getFinishedAt() != null) {
                updatedAt = training.getFinishedAt();
            }

            /*
             * 단계 하락 방지:
             * 매 완료 시점의 조건을 평가하고 최고 단계만 보존한다.
             * 따라서 최근 정확도가 낮아져도 이미 핀 꽃이 이전 단계로 되돌아가지 않는다.
             */
            highestStage = Math.max(highestStage, currentStage());
        }

        private int currentStage() {
            BigDecimal recentAverage = recentAverageAccuracy();
            if (completedCount >= properties.fullBloomCompleted()
                    && percentageAtLeast(
                            masteredTemplates.size(),
                            properties.fullBloomMasteryCoveragePercent()
                    )
                    && atLeast(
                            recentAverage,
                            properties.fullBloomRecentAccuracy()
                    )) {
                return 5;
            }
            if (completedCount >= properties.flowerCompleted()
                    && percentageAtLeast(
                            experiencedTemplates.size(),
                            properties.flowerCoveragePercent()
                    )
                    && atLeast(recentAverage, properties.flowerRecentAccuracy())) {
                return 4;
            }
            if (completedCount >= properties.budCompleted()
                    && percentageAtLeast(
                            experiencedTemplates.size(),
                            properties.budCoveragePercent()
                    )) {
                return 3;
            }
            if (completedCount >= properties.sproutCompleted()
                    && experiencedTemplates.size()
                    >= properties.sproutDistinctTemplates()) {
                return 2;
            }
            return 1;
        }

        private boolean percentageAtLeast(int count, int thresholdPercent) {
            return (long) count * 100
                    >= (long) area.totalTemplateCount() * thresholdPercent;
        }

        private boolean atLeast(BigDecimal value, int threshold) {
            return value != null
                    && value.compareTo(BigDecimal.valueOf(threshold)) >= 0;
        }

        private BigDecimal recentAverageAccuracy() {
            if (recentAccuracies.isEmpty()) {
                return null;
            }
            return recentAccuracies.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(
                            BigDecimal.valueOf(recentAccuracies.size()),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        private int percentage(int count) {
            // 은퇴 템플릿으로 훈련한 이력이 남아 있으면 분자가 분모를 넘을 수 있다.
            return Math.min(100, BigDecimal.valueOf(count * 100L)
                    .divide(
                            BigDecimal.valueOf(area.totalTemplateCount()),
                            0,
                            RoundingMode.HALF_UP
                    )
                    .intValueExact());
        }

        private GrowthAreaResponse response() {
            NextStageProgress nextStage = nextStageProgress();
            return new GrowthAreaResponse(
                    area.id,
                    area.displayName,
                    highestStage,
                    STAGE_NAMES.get(highestStage - 1),
                    completedCount,
                    experiencedTemplates.size(),
                    area.totalTemplateCount(),
                    percentage(experiencedTemplates.size()),
                    masteredTemplates.size(),
                    percentage(masteredTemplates.size()),
                    recentAverageAccuracy(),
                    nextStage.percent(),
                    nextStage.hint(),
                    updatedAt
            );
        }

        /**
         * 다음 단계 승급 조건 대비 진행률.
         * 병목(가장 덜 채워진) 조건을 기준으로 하므로, 진행 바가 100%가 되는
         * 순간이 곧 승급 시점과 일치한다. 힌트도 병목 조건을 설명한다.
         */
        private NextStageProgress nextStageProgress() {
            if (highestStage >= STAGE_NAMES.size()) {
                return new NextStageProgress(100, null);
            }
            List<NextStageCondition> conditions = switch (highestStage + 1) {
                case 2 -> List.of(
                        completedCondition(properties.sproutCompleted()),
                        distinctCondition(properties.sproutDistinctTemplates())
                );
                case 3 -> List.of(
                        completedCondition(properties.budCompleted()),
                        coverageCondition(properties.budCoveragePercent())
                );
                case 4 -> List.of(
                        completedCondition(properties.flowerCompleted()),
                        coverageCondition(properties.flowerCoveragePercent()),
                        accuracyCondition(properties.flowerRecentAccuracy())
                );
                default -> List.of(
                        completedCondition(properties.fullBloomCompleted()),
                        masteryCondition(properties.fullBloomMasteryCoveragePercent()),
                        accuracyCondition(properties.fullBloomRecentAccuracy())
                );
            };
            NextStageCondition bottleneck = conditions.stream()
                    .min(Comparator.comparingInt(NextStageCondition::percent))
                    .orElseThrow();
            return new NextStageProgress(
                    bottleneck.percent(),
                    bottleneck.percent() >= 100 ? null : bottleneck.hint()
            );
        }

        private NextStageCondition completedCondition(int required) {
            long remaining = Math.max(0, required - completedCount);
            return new NextStageCondition(
                    ratioPercent(completedCount, required),
                    "훈련을 " + remaining + "번 더 하면 자라나요!"
            );
        }

        private NextStageCondition distinctCondition(int required) {
            int remaining = Math.max(0, required - experiencedTemplates.size());
            return new NextStageCondition(
                    ratioPercent(experiencedTemplates.size(), required),
                    "새로운 활동을 " + remaining + "개 더 해봐요!"
            );
        }

        private NextStageCondition coverageCondition(int requiredPercent) {
            int requiredCount = requiredTemplateCount(requiredPercent);
            int remaining = Math.max(0, requiredCount - experiencedTemplates.size());
            return new NextStageCondition(
                    ratioPercent(experiencedTemplates.size(), requiredCount),
                    "새로운 활동을 " + remaining + "개 더 해봐요!"
            );
        }

        private NextStageCondition masteryCondition(int requiredPercent) {
            int requiredCount = requiredTemplateCount(requiredPercent);
            int remaining = Math.max(0, requiredCount - masteredTemplates.size());
            return new NextStageCondition(
                    ratioPercent(masteredTemplates.size(), requiredCount),
                    "활동 " + remaining + "개를 더 멋지게 해내 봐요!"
            );
        }

        private NextStageCondition accuracyCondition(int requiredAccuracy) {
            BigDecimal recentAverage = recentAverageAccuracy();
            long achieved = recentAverage == null
                    ? 0
                    : recentAverage.setScale(0, RoundingMode.DOWN).longValueExact();
            return new NextStageCondition(
                    ratioPercent(achieved, requiredAccuracy),
                    "요즘 점수를 " + requiredAccuracy + "점까지 올려봐요!"
            );
        }

        private int requiredTemplateCount(int requiredPercent) {
            return Math.toIntExact(BigDecimal
                    .valueOf((long) area.totalTemplateCount() * requiredPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                    .longValueExact());
        }

        private static int ratioPercent(long achieved, long required) {
            if (required <= 0) {
                return 100;
            }
            return Math.toIntExact(Math.min(100, achieved * 100 / required));
        }
    }

    private record NextStageProgress(int percent, String hint) {
    }

    private record NextStageCondition(int percent, String hint) {
    }
}
