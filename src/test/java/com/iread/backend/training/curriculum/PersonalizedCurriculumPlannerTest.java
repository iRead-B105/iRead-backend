package com.iread.backend.training.curriculum;

import com.iread.backend.readingfeature.domain.ReadingFeatureCategory;
import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import com.iread.backend.readingfeature.domain.ReadingFeatureScope;
import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.training.domain.CurriculumUnitEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalizedCurriculumPlannerTest {

    @Test
    void selectsThreeDirectOneExtensionAndOneFluencyTraining() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles =
                mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula,
                templates,
                profiles,
                students,
                testCurriculums,
                trainingData,
                generation,
                JsonMapper.builder().build()
        );

        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                1L, null, "GRAPHEME.CODA.SIMPLE.ㄱ", "받침 ㄱ",
                ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER
        );
        StudentFeatureProfileEntity profile = new StudentFeatureProfileEntity(
                1L, mock(com.iread.backend.student.domain.StudentEntity.class),
                feature, new BigDecimal("0.9000")
        );
        profile.updateMetrics(
                new BigDecimal("0.3000"), 500, new BigDecimal("0.70"),
                1200, new BigDecimal("3.00"), new BigDecimal("2.00"),
                BigDecimal.ZERO, 2500, 800, new BigDecimal("0.9000"),
                10, null, null
        );

        List<TrainingTemplateEntity> catalog = List.of(
                template(1L, 1, 1, true),
                template(2L, 1, 2, true),
                template(3L, 1, 3, true),
                template(4L, 4, 1, false),
                template(5L, 8, 1, false),
                template(6L, 1, 4, false)
        );
        when(templates.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(catalog);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L))
                .thenReturn(List.of(profile));

        List<Long> selected = planner.selectTemplates(15L).stream()
                .map(TrainingTemplateEntity::getId)
                .toList();

        assertThat(selected).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void createsFiveTrainingsLinkedToCompletedSourceTestOnlyOnce() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula,
                templates,
                profiles,
                students,
                testCurriculums,
                trainingData,
                generation,
                JsonMapper.builder().build()
        );
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(source.getStatus()).thenReturn(TestStatus.COMPLETED.name());
        when(students.findByIdForUpdate(15L)).thenReturn(Optional.of(student));
        when(curricula.findBySourceTestCurriculumId(500L)).thenReturn(Optional.empty());
        when(testCurriculums.findByIdForUpdate(500L)).thenReturn(Optional.of(source));
        when(curricula.findByStudentIdAndStatus(15L, DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(Optional.empty());
        when(curricula.existsByStudentId(15L)).thenReturn(true);
        List<TrainingTemplateEntity> catalog = List.of(
                template(1L, 1, 1, false),
                template(2L, 2, 1, false),
                template(3L, 3, 1, false),
                template(4L, 4, 1, false),
                template(5L, 8, 1, false),
                template(6L, 5, 1, false)
        );
        when(templates.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(catalog);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L)).thenReturn(List.of());
        when(curricula.saveAndFlush(any(DailyCurriculumEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyCurriculumEntity created = planner.createRecommendedFromTestIfAbsent(student, 500L);

        assertThat(created.getSourceTestCurriculum()).isSameAs(source);
        assertThat(created.getTrainings()).hasSize(5);
        assertThat(created.getTrainings()).extracting(training -> training.getSequenceNo())
                .containsExactly(1, 2, 3, 4, 5);
        verify(generation, never()).generateSeed(any());
    }

    @Test
    void seedsFirstCurriculumWithoutAiCalls() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula,
                templates,
                profiles,
                students,
                testCurriculums,
                trainingData,
                generation,
                JsonMapper.builder().build()
        );
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        when(students.findByIdForUpdate(15L)).thenReturn(Optional.of(student));
        when(curricula.findByStudentIdAndStatus(15L, DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(Optional.empty());
        when(curricula.existsByStudentId(15L)).thenReturn(false);
        List<TrainingTemplateEntity> catalog = List.of(
                template(1L, 1, 1, false),
                template(2L, 2, 1, false),
                template(3L, 3, 1, false),
                template(4L, 4, 1, false),
                template(5L, 8, 1, false),
                template(6L, 5, 1, false)
        );
        when(templates.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(catalog);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L)).thenReturn(List.of());
        when(curricula.saveAndFlush(any(DailyCurriculumEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(generation.generateSeed(any())).thenAnswer(invocation ->
                JsonMapper.builder().build().createObjectNode().put("schemaVersion", 2));

        DailyCurriculumEntity created = planner.createNextIfAbsent(student);

        assertThat(created.getTrainings()).hasSize(5);
        assertThat(created.getTrainings()).allMatch(training ->
                training.getStatus() == com.iread.backend.training.domain.TrainingStatus.NOT_STARTED);
        verify(generation, times(5)).generateSeed(any());
        verify(trainingData, times(5)).save(any());
    }

    @Test
    void doesNotReuseUnrelatedNotStartedCurriculum() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula,
                templates,
                profiles,
                students,
                testCurriculums,
                trainingData,
                generation,
                JsonMapper.builder().build()
        );
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(source.getStatus()).thenReturn(TestStatus.COMPLETED.name());
        when(students.findByIdForUpdate(15L)).thenReturn(Optional.of(student));
        when(curricula.findBySourceTestCurriculumId(500L)).thenReturn(Optional.empty());
        when(testCurriculums.findByIdForUpdate(500L)).thenReturn(Optional.of(source));
        when(curricula.findByStudentIdAndStatus(15L, DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(Optional.of(mock(DailyCurriculumEntity.class)));

        assertThatThrownBy(() -> planner.createRecommendedFromTestIfAbsent(student, 500L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다른 출처");
        verify(curricula, never()).saveAndFlush(any());
    }

    @Test
    void returnsExistingRecommendationForTheSameSourceTest() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula,
                templates,
                profiles,
                students,
                testCurriculums,
                trainingData,
                generation,
                JsonMapper.builder().build()
        );
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        DailyCurriculumEntity existing = mock(DailyCurriculumEntity.class);
        when(students.findByIdForUpdate(15L)).thenReturn(Optional.of(student));
        when(curricula.findBySourceTestCurriculumId(500L)).thenReturn(Optional.of(existing));

        assertThat(planner.createRecommendedFromTestIfAbsent(student, 500L)).isSameAs(existing);

        verify(testCurriculums, never()).findByIdForUpdate(500L);
        verify(curricula, never()).saveAndFlush(any());
    }

    private TrainingTemplateEntity template(
            Long id,
            int unitSequence,
            int sequence,
            boolean compatible
    ) {
        CurriculumUnitEntity unit = mock(CurriculumUnitEntity.class);
        when(unit.getSequenceNo()).thenReturn(unitSequence);
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn(id);
        when(template.getCurriculumUnit()).thenReturn(unit);
        when(template.getSequenceNo()).thenReturn(sequence);
        when(template.getPrompt()).thenReturn(compatible
                ? """
                  {"supportedFeatureCategories":["GRAPHEME"],
                   "supportedScopes":["CHARACTER"]}
                  """
                : """
                  {"supportedFeatureCategories":["SENTENCE"],
                   "supportedScopes":["SENTENCE"]}
                  """);
        return template;
    }
}
