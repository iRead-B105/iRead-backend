package com.iread.backend.test.admin.result;

import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestRecommendationStatus;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestCurriculumResultAggregatorTest {
    private TestQuestionResultAssembler questionAssembler;
    private TestCurriculumResultAggregator aggregator;

    @BeforeEach
    void setUp() {
        questionAssembler = mock(TestQuestionResultAssembler.class);
        aggregator = new TestCurriculumResultAggregator(
                questionAssembler,
                new TestScoreNormalizer(),
                new TestTrackResolver()
        );
    }

    @Test
    void aggregatesNineQuestionsIntoOverallAndThreeAreaScores() {
        TestCurriculumEntity curriculum = curriculum("COMPLETED");
        List<StudentTestEntity> tests = new ArrayList<>();
        for (int sequence = 1; sequence <= 9; sequence++) {
            StudentTestEntity test = test((long) sequence, sequence, TestStatus.COMPLETED);
            tests.add(test);
            BigDecimal score = BigDecimal.valueOf(sequence * 10L);
            when(questionAssembler.assembleAll(test)).thenReturn(List.of(question(
                    sequence,
                    score,
                    sequence == 1 ? 0L : 10L
            )));
        }

        var result = aggregator.aggregate(curriculum, tests);

        assertThat(result.completedQuestions()).isEqualTo(9);
        assertThat(result.totalQuestions()).isEqualTo(9);
        assertThat(result.overallScore()).isEqualByComparingTo("50.00");
        assertThat(result.areaScores()).extracting(TestCurriculumDetailResponse.AreaScore::score)
                .containsExactly(
                        new BigDecimal("20.00"),
                        new BigDecimal("50.00"),
                        new BigDecimal("80.00")
                );
        assertThat(result.solvingTimeSeconds()).isEqualTo(80L);
        assertThat(result.questions()).extracting(
                TestCurriculumDetailResponse.QuestionResult::sequenceNo
        ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(result.dailyCurriculumId()).isNull();
        assertThat(result.teacherReviewStatus()).isNull();
    }

    @Test
    void aggregatesThreeLegacyTestsWithThreeQuestionsEach() {
        TestCurriculumEntity curriculum = curriculum("COMPLETED");
        List<StudentTestEntity> tests = new ArrayList<>();
        for (int testSequence = 1; testSequence <= 3; testSequence++) {
            StudentTestEntity test = test((long) testSequence, testSequence, TestStatus.COMPLETED);
            tests.add(test);
            int firstQuestion = (testSequence - 1) * 3 + 1;
            when(questionAssembler.assembleAll(test)).thenReturn(List.of(
                    question(firstQuestion, BigDecimal.valueOf(50), 10L),
                    question(firstQuestion + 1, BigDecimal.valueOf(60), null),
                    question(firstQuestion + 2, BigDecimal.valueOf(70), null)
            ));
        }

        var result = aggregator.aggregate(curriculum, tests);

        assertThat(result.completedQuestions()).isEqualTo(9);
        assertThat(result.totalQuestions()).isEqualTo(9);
        assertThat(result.solvingTimeSeconds()).isEqualTo(30L);
        assertThat(result.questions()).extracting(
                TestCurriculumDetailResponse.QuestionResult::sequenceNo
        ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
    @Test
    void rejectsCompletedCurriculumWithFewerThanNineCompletedQuestions() {
        TestCurriculumEntity curriculum = curriculum("COMPLETED");
        List<StudentTestEntity> tests = List.of(
                test(1L, 1, TestStatus.COMPLETED),
                test(2L, 2, TestStatus.COMPLETED)
        );

        assertThatThrownBy(() -> aggregator.aggregate(curriculum, tests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9개 문항");
    }

    @Test
    void exposesRecommendationProcessingStateAndLinkedCurriculum() {
        TestCurriculumEntity curriculum = curriculum("IN_PROGRESS");
        when(curriculum.getRecommendationStatus())
                .thenReturn(TestRecommendationStatus.FAILED);
        when(curriculum.getRecommendationError()).thenReturn("추천 실패");
        when(curriculum.getRecommendationRetryCount()).thenReturn(2);
        DailyCurriculumEntity recommendation = mock(DailyCurriculumEntity.class);
        when(recommendation.getId()).thenReturn(700L);

        var result = aggregator.aggregate(curriculum, List.of(), recommendation);

        assertThat(result.recommendationStatus()).isEqualTo("FAILED");
        assertThat(result.recommendationError()).isEqualTo("추천 실패");
        assertThat(result.recommendationRetryCount()).isEqualTo(2);
        assertThat(result.dailyCurriculumId()).isEqualTo(700L);
    }

    private TestCurriculumEntity curriculum(String status) {
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        when(curriculum.getId()).thenReturn(100L);
        when(curriculum.getStatus()).thenReturn(status);
        when(curriculum.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 9, 0));
        when(curriculum.getCompletedAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 10, 0));
        return curriculum;
    }

    private StudentTestEntity test(Long id, int sequenceNo, TestStatus status) {
        StudentTestEntity test = mock(StudentTestEntity.class);
        when(test.getId()).thenReturn(id);
        when(test.getSequenceNo()).thenReturn(sequenceNo);
        when(test.getStatus()).thenReturn(status);
        when(test.getAccuracy()).thenReturn(BigDecimal.valueOf(sequenceNo * 10L));
        return test;
    }

    private TestCurriculumDetailResponse.QuestionResult question(
            int sequenceNo,
            BigDecimal score,
            Long solvingTime
    ) {
        String track = new TestTrackResolver().resolve(sequenceNo).code();
        return new TestCurriculumDetailResponse.QuestionResult(
                (long) sequenceNo,
                sequenceNo,
                track,
                "TYPE",
                "문항 " + sequenceNo,
                "SINGLE_CHOICE",
                null,
                null,
                true,
                score,
                null,
                solvingTime,
                null
        );
    }
}
