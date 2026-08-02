package com.iread.backend.wordattempt.domain;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.training.domain.WordEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WordAttemptLogEntityTest {

    @Test
    void marksAttemptAsHavingGazeDataWhenMetricsAreMerged() {
        WordAttemptLogEntity attempt = WordAttemptLogEntity.forTest(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                mock(StudentTestEntity.class),
                "학교",
                true,
                900,
                100,
                400,
                false,
                true,
                900,
                1,
                0,
                0
        );

        attempt.applyGazeMetrics(500, 2, 120, 620, false, 0, 850);

        assertThat(attempt.isHasGazeData()).isTrue();
        assertThat(attempt.getFixationDurationMs()).isEqualTo(500);
        assertThat(attempt.getTotalScore()).isEqualTo(850);
    }
}
