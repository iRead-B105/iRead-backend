package com.iread.backend.test.admin.result;

import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.admin.dto.res.TestCurriculumListResponse;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TestCurriculumResultAggregator {
    private final TestQuestionResultAssembler questionAssembler;
    private final TestScoreNormalizer scoreNormalizer;
    private final TestTrackResolver trackResolver;

    public TestCurriculumListResponse.Item summarize(
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> tests
    ) {
        List<StudentTestEntity> ordered = ordered(tests);
        int completed = completedCount(ordered);
        BigDecimal overallScore = scoreNormalizer.average(ordered.stream()
                .filter(test -> test.getStatus() == TestStatus.COMPLETED)
                .map(StudentTestEntity::getAccuracy)
                .map(this::normalizeStoredAccuracy)
                .toList());
        return new TestCurriculumListResponse.Item(
                curriculum.getId(),
                curriculum.getStatus(),
                curriculum.getCreatedAt(),
                curriculum.getCompletedAt(),
                completed,
                ordered.size(),
                overallScore
        );
    }

    public TestCurriculumDetailResponse aggregate(
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> tests
    ) {
        return aggregate(curriculum, tests, null);
    }

    public TestCurriculumDetailResponse aggregate(
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> tests,
            DailyCurriculumEntity recommendedCurriculum
    ) {
        List<StudentTestEntity> ordered = ordered(tests);
        List<TestCurriculumDetailResponse.QuestionResult> assembled = new ArrayList<>();
        int completedQuestions = 0;
        for (StudentTestEntity test : ordered) {
            List<TestCurriculumDetailResponse.QuestionResult> testQuestions =
                    questionAssembler.assembleAll(test);
            assembled.addAll(testQuestions);
            if (test.getStatus() == TestStatus.COMPLETED) {
                completedQuestions += testQuestions.size();
            }
        }
        List<TestCurriculumDetailResponse.QuestionResult> questions = assembled.stream()
                .sorted(Comparator.comparingInt(
                        TestCurriculumDetailResponse.QuestionResult::sequenceNo
                ))
                .toList();
        validateCompletedCurriculum(curriculum, ordered, questions);
        if (isCompleted(curriculum) && questions.stream().anyMatch(q -> q.score() == null)) {
            throw new IllegalStateException("완료된 실력도전 검사에 점수가 없는 문항이 있습니다.");
        }

        return new TestCurriculumDetailResponse(
                curriculum.getId(),
                curriculum.getStatus(),
                curriculum.getCreatedAt(),
                curriculum.getCompletedAt(),
                completedQuestions,
                questions.size(),
                scoreNormalizer.average(questions.stream()
                        .map(TestCurriculumDetailResponse.QuestionResult::score)
                        .toList()),
                areaScores(questions),
                totalSolvingTime(questions),
                questions,
                curriculum.getRecommendationStatus() == null
                        ? null
                        : curriculum.getRecommendationStatus().name(),
                curriculum.getRecommendationError(),
                curriculum.getRecommendationLastAttemptAt(),
                curriculum.getRecommendationRetryCount(),
                recommendedCurriculum == null ? null : recommendedCurriculum.getId(),
                contentGenerationStatus(recommendedCurriculum),
                recommendedCurriculum == null || recommendedCurriculum.getReviewStatus() == null
                        ? null
                        : recommendedCurriculum.getReviewStatus().name()
        );
    }

    private String contentGenerationStatus(DailyCurriculumEntity curriculum) {
        if (curriculum == null || curriculum.getTrainings().isEmpty()) {
            return null;
        }
        List<String> statuses = curriculum.getTrainings().stream()
                .map(training -> training.getStatus().name())
                .distinct()
                .toList();
        return statuses.size() == 1 ? statuses.get(0) : "MIXED";
    }

    private List<TestCurriculumDetailResponse.AreaScore> areaScores(
            List<TestCurriculumDetailResponse.QuestionResult> questions
    ) {
        return List.of(1, 4, 7).stream().map(firstSequence -> {
            TestTrackResolver.Track track = trackResolver.resolve(firstSequence);
            List<TestCurriculumDetailResponse.QuestionResult> trackQuestions = questions.stream()
                    .filter(question -> question.trackCode().equals(track.code()))
                    .toList();
            int completed = (int) trackQuestions.stream()
                    .filter(question -> question.score() != null)
                    .count();
            return new TestCurriculumDetailResponse.AreaScore(
                    track.code(),
                    track.title(),
                    scoreNormalizer.average(trackQuestions.stream()
                            .map(TestCurriculumDetailResponse.QuestionResult::score)
                            .toList()),
                    completed,
                    TestTrackResolver.QUESTIONS_PER_TRACK
            );
        }).toList();
    }

    private Long totalSolvingTime(
            List<TestCurriculumDetailResponse.QuestionResult> questions
    ) {
        List<Long> measured = questions.stream()
                .map(TestCurriculumDetailResponse.QuestionResult::solvingTimeSeconds)
                .filter(value -> value != null)
                .toList();
        return measured.isEmpty() ? null : measured.stream().mapToLong(Long::longValue).sum();
    }

    private void validateCompletedCurriculum(
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> tests,
            List<TestCurriculumDetailResponse.QuestionResult> questions
    ) {
        if (!isCompleted(curriculum)) {
            return;
        }
        if (questions.size() != TestTrackResolver.TOTAL_QUESTIONS
                || tests.stream().anyMatch(test -> test.getStatus() != TestStatus.COMPLETED)) {
            throw new IllegalStateException("완료된 실력도전 검사는 9개 문항이 모두 완료되어야 합니다.");
        }
    }

    private boolean isCompleted(TestCurriculumEntity curriculum) {
        return TestStatus.COMPLETED.name().equals(curriculum.getStatus());
    }

    private int completedCount(List<StudentTestEntity> tests) {
        return (int) tests.stream()
                .filter(test -> test.getStatus() == TestStatus.COMPLETED)
                .count();
    }

    private List<StudentTestEntity> ordered(List<StudentTestEntity> tests) {
        return tests.stream()
                .sorted(Comparator.comparingInt(StudentTestEntity::getSequenceNo)
                        .thenComparing(StudentTestEntity::getId))
                .toList();
    }

    private BigDecimal normalizeStoredAccuracy(BigDecimal accuracy) {
        if (accuracy == null) {
            return null;
        }
        BigDecimal normalized = accuracy.compareTo(BigDecimal.valueOf(100)) > 0
                ? accuracy.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
                : accuracy;
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }
}
