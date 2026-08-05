package com.iread.backend.test.domain;

import com.iread.backend.student.domain.StudentEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TestCurriculumEntityTest {

    @Test
    void completionRequestsRecommendationOnceAndFailureCanBeRetried() {
        TestCurriculumEntity curriculum = new TestCurriculumEntity(
                500L,
                mock(StudentEntity.class),
                LocalDateTime.of(2026, 8, 1, 9, 0)
        );
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);

        assertThat(curriculum.complete(completedAt)).isTrue();
        assertThat(curriculum.complete(completedAt.plusMinutes(1))).isFalse();
        assertThat(curriculum.getCompletedAt()).isEqualTo(completedAt);
        assertThat(curriculum.getRecommendationStatus())
                .isEqualTo(TestRecommendationStatus.PENDING);

        assertThat(curriculum.startRecommendation(completedAt.plusSeconds(1))).isTrue();
        assertThat(curriculum.startRecommendation(completedAt.plusSeconds(2))).isFalse();
        assertThat(curriculum.getRecommendationRetryCount()).isEqualTo(1);
        curriculum.failRecommendation("추천 실패");
        assertThat(curriculum.getRecommendationStatus())
                .isEqualTo(TestRecommendationStatus.FAILED);
        assertThat(curriculum.requestRecommendationRetry()).isTrue();
        assertThat(curriculum.startRecommendation(completedAt.plusSeconds(3))).isTrue();
        assertThat(curriculum.getRecommendationRetryCount()).isEqualTo(2);
        curriculum.completeRecommendation();
        assertThat(curriculum.getRecommendationStatus())
                .isEqualTo(TestRecommendationStatus.COMPLETED);
        assertThat(curriculum.getRecommendationError()).isNull();
    }
}
