package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.CurriculumUnitEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTrainingCatalogServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock DailyCurriculumRepository dailyCurriculumRepository;
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @InjectMocks AppTrainingCatalogService service;

    @Test
    void returnsAuthenticatedStudentsCurrentCurriculumTrainings() {
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        CurriculumUnitEntity unit = mock(CurriculumUnitEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.empty());
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.NOT_STARTED
        )).thenReturn(Optional.of(curriculum));
        when(curriculum.getId()).thenReturn(30L);
        when(curriculum.getStatus()).thenReturn(DailyCurriculumStatus.NOT_STARTED);
        when(curriculum.getTrainings()).thenReturn(List.of(training));
        when(training.getId()).thenReturn(40L);
        when(training.getSequenceNo()).thenReturn(1);
        when(training.getStatus()).thenReturn(TrainingStatus.NOT_STARTED);
        when(training.getTrainingTemplate()).thenReturn(template);
        when(template.getId()).thenReturn(50L);
        when(template.getName()).thenReturn("문장 따라 읽기");
        when(template.getPrompt()).thenReturn("{\"trainingType\":\"SENTENCE_REPEAT\"}");
        when(template.getCurriculumUnit()).thenReturn(unit);
        when(unit.getUnitName()).thenReturn("유창성");

        var response = service.getCurrentTrainingList(1L, 20L);

        assertThat(response.curriculumId()).isEqualTo(30L);
        assertThat(response.curriculumStatus())
                .isEqualTo(DailyCurriculumStatus.NOT_STARTED);
        assertThat(response.trainings()).singleElement().satisfies(item -> {
            assertThat(item.trainingId()).isEqualTo(40L);
            assertThat(item.trainingTemplateId()).isEqualTo(50L);
            assertThat(item.trainingType().name()).isEqualTo("SENTENCE_REPEAT");
            assertThat(item.sequenceNo()).isEqualTo(1);
            assertThat(item.unitName()).isEqualTo("유창성");
            assertThat(item.trainingName()).isEqualTo("문장 따라 읽기");
            assertThat(item.status()).isEqualTo(TrainingStatus.NOT_STARTED);
        });
    }

    @Test
    void prefersInProgressCurriculumOverNotStartedCurriculum() {
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity inProgress = mock(DailyCurriculumEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.of(inProgress));
        when(inProgress.getId()).thenReturn(31L);
        when(inProgress.getStatus()).thenReturn(DailyCurriculumStatus.IN_PROGRESS);
        when(inProgress.getTrainings()).thenReturn(List.of());

        var response = service.getCurrentTrainingList(1L, 20L);

        assertThat(response.curriculumId()).isEqualTo(31L);
        assertThat(response.curriculumStatus()).isEqualTo(DailyCurriculumStatus.IN_PROGRESS);
    }

    @Test
    void hidesRetiredClassificationTrainingFromAnExistingCurriculum() {
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        TrainingEntity retiredTraining = mock(TrainingEntity.class);
        TrainingTemplateEntity retiredTemplate = mock(TrainingTemplateEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.of(curriculum));
        when(curriculum.getId()).thenReturn(31L);
        when(curriculum.getStatus()).thenReturn(DailyCurriculumStatus.IN_PROGRESS);
        when(curriculum.getTrainings()).thenReturn(List.of(retiredTraining));
        when(retiredTraining.getTrainingTemplate()).thenReturn(retiredTemplate);
        when(retiredTemplate.getId()).thenReturn(6L);

        var response = service.getCurrentTrainingList(1L, 20L);

        assertThat(response.trainings()).isEmpty();
    }

    @Test
    void rejectsStudentOutsideAuthenticatedTeacher() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentTrainingList(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("학생을 찾을 수 없습니다.");
        verifyNoInteractions(dailyCurriculumRepository);
    }

    @Test
    void hidesUnreviewedTestRecommendedCurriculumFromStudentCatalog() {
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.empty());
        when(dailyCurriculumRepository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.NOT_STARTED
        )).thenReturn(Optional.of(curriculum));
        when(curriculum.isRecommendedFromTest()).thenReturn(true);
        when(curriculum.isAvailableToStudent()).thenReturn(false);

        assertThatThrownBy(() -> service.getCurrentTrainingList(1L, 20L))
                .isInstanceOfSatisfying(ResourceNotFoundException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("ACTIVE_CURRICULUM_NOT_FOUND");
                    assertThat(exception).hasMessage("현재 진행 가능한 커리큘럼을 찾을 수 없습니다.");
                });
    }
}
