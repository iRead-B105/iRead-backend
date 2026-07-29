package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TrainingEntityTest {

    @Test
    void startingTrainingMarksCurriculumInProgress() {
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(
                mock(StudentEntity.class),
                List.of(mock(TrainingTemplateEntity.class))
        );
        TrainingEntity training = curriculum.getTrainings().getFirst();
        training.markReady();

        training.start(LocalDateTime.of(2026, 7, 29, 12, 0));

        assertThat(training.getStatus()).isEqualTo(TrainingStatus.IN_PROGRESS);
        assertThat(curriculum.getStatus())
                .isEqualTo(DailyCurriculumStatus.IN_PROGRESS);
    }
}
