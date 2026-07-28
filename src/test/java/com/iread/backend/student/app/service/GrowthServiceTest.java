package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @InjectMocks GrowthService growthService;

    @Test
    void returnsCompletedCountByTrainingTemplate() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingRepository.TrainingProgressProjection projection =
                mock(TrainingRepository.TrainingProgressProjection.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(projection.getTrainingTemplateId()).thenReturn(100L);
        when(projection.getTrainingTemplateName()).thenReturn("낱말 읽기");
        when(projection.getCompletedCount()).thenReturn(3L);
        when(trainingRepository.findCompletedTrainingProgress(20L))
                .thenReturn(List.of(projection));

        var result = growthService.getGrowth(1L, 20L);

        assertThat(result.trainingProgress()).singleElement().satisfies(progress -> {
            assertThat(progress.trainingTemplateId()).isEqualTo(100L);
            assertThat(progress.trainingTemplateName()).isEqualTo("낱말 읽기");
            assertThat(progress.completedCount()).isEqualTo(3L);
        });
    }

    @Test
    void rejectsStudentOutsideTeacherOwnership() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> growthService.getGrowth(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(trainingRepository);
    }
}
