package com.iread.backend.training.app.service;

import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoLearningClockTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final TrainingRepository trainingRepository = mock(TrainingRepository.class);
    private final DemoLearningClock learningClock = new DemoLearningClock(
            trainingRepository,
            Clock.fixed(Instant.parse("2026-08-03T01:30:00Z"), SEOUL)
    );

    @Test
    void usesTheLatestPersistedFutureCompletionAsTheLogicalDate() {
        when(trainingRepository.findLatestFinishedAtByStudentId(2103L))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 8, 5, 9, 15)));

        assertThat(learningClock.currentDate(2103L)).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(learningClock.currentDateTime(2103L))
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 30));
        assertThat(learningClock.nextDateTime(2103L))
                .isEqualTo(LocalDateTime.of(2026, 8, 6, 10, 30));
    }

    @Test
    void neverMovesBehindTheRealDate() {
        when(trainingRepository.findLatestFinishedAtByStudentId(2103L))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 8, 1, 9, 15)));

        assertThat(learningClock.currentDate(2103L)).isEqualTo(LocalDate.of(2026, 8, 3));
    }
}
