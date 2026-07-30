package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TrainingEntityTest {

    @Test
    void generatedTrainingIsLockedBeforeLearnerStarts() {
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(
                mock(StudentEntity.class),
                List.of(mock(TrainingTemplateEntity.class))
        );
        TrainingEntity training = curriculum.getTrainings().getFirst();

        assertThat(training.isEditable()).isTrue();

        training.markReady();

        assertThat(training.getStatus()).isEqualTo(TrainingStatus.NOT_STARTED);
        assertThat(training.isEditable()).isFalse();

        training.start(LocalDateTime.of(2026, 7, 30, 13, 0));

        assertThat(training.isEditable()).isFalse();
    }

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
