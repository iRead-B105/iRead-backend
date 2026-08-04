package com.iread.backend.training.config;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoAllTrainingCurriculumInitializerTest {

    /** 기준 템플릿 34개 중 은퇴 템플릿(6, 14, 24)을 제외한 진행 가능 훈련 수 */
    private static final int SELECTABLE_TEMPLATE_COUNT = 31;

    @Test
    void createsSelectableCatalogTrainingsAndUnlocksOnlyTheFirst() throws Exception {
        DailyCurriculumRepository curriculumRepository =
                mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templateRepository =
                mock(TrainingTemplateRepository.class);
        TrainingDataRepository dataRepository =
                mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generationService =
                mock(PersonalizedTrainingGenerationService.class);
        ObjectMapper objectMapper = JsonMapper.builder().build();

        StudentEntity student = StudentEntity.builder().name("샛별").build();
        ReflectionTestUtils.setField(student, "id", 2001L);
        List<TrainingTemplateEntity> previousTemplates = List.of(template(1), template(2));
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(student, previousTemplates);
        ReflectionTestUtils.setField(
                curriculum,
                "id",
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        );
        ReflectionTestUtils.setField(curriculum.getTrainings().get(0), "id", 91L);
        ReflectionTestUtils.setField(curriculum.getTrainings().get(1), "id", 92L);
        List<TrainingTemplateEntity> templates = canonicalTemplates();

        when(curriculumRepository.findForGeneration(
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(templateRepository.findCanonicalCatalog(1L, 34L)).thenReturn(templates);
        when(generationService.generate(any())).thenAnswer(invocation -> {
            ObjectNode generated = objectMapper.createObjectNode();
            generated.put("schemaVersion", 2);
            IntStream.rangeClosed(1, 5).forEach(questionNo -> generated.withArray("questions")
                    .addObject().put("questionNo", questionNo).put("type", "VOWEL_SOUND_CHOICE"));
            return generated;
        });

        DemoAllTrainingCurriculumInitializer initializer =
                new DemoAllTrainingCurriculumInitializer(
                        curriculumRepository,
                        templateRepository,
                        dataRepository,
                        generationService,
                        objectMapper
                );

        initializer.run(mock(org.springframework.boot.ApplicationArguments.class));

        assertThat(curriculum.getTrainings()).hasSize(SELECTABLE_TEMPLATE_COUNT);
        assertThat(curriculum.getTrainings())
                .allMatch(training -> TrainingCatalogPolicy.isSelectable(
                        training.getTrainingTemplate()
                ));
        assertThat(curriculum.getTrainings().getFirst().getStatus())
                .isEqualTo(TrainingStatus.NOT_STARTED);
        assertThat(curriculum.getTrainings().subList(
                1,
                SELECTABLE_TEMPLATE_COUNT
        )).allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);

        ArgumentCaptor<TrainingDataEntity> captor =
                ArgumentCaptor.forClass(TrainingDataEntity.class);
        verify(
                dataRepository,
                times(SELECTABLE_TEMPLATE_COUNT)
        ).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(data -> {
            try {
                assertThat(objectMapper.readTree(data.getGeneratedData()).path("questions"))
                        .hasSize(5);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        verify(curriculumRepository, times(2)).flush();
        verify(dataRepository, times(2)).flush();
        verify(dataRepository).deleteByTrainingId(91L);
        verify(dataRepository).deleteByTrainingId(92L);
    }

    @Test
    void preservesQuestionsAndProgressForExistingCatalogTraining() {
        DailyCurriculumRepository curriculumRepository =
                mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templateRepository =
                mock(TrainingTemplateRepository.class);
        TrainingDataRepository dataRepository =
                mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generationService =
                mock(PersonalizedTrainingGenerationService.class);
        ObjectMapper objectMapper = JsonMapper.builder().build();

        StudentEntity student = StudentEntity.builder().name("샛별").build();
        ReflectionTestUtils.setField(student, "id", 2001L);
        List<TrainingTemplateEntity> templates = canonicalTemplates();
        // 이미 은퇴 템플릿이 제외된 상태의 커리큘럼은 재생성하지 않아야 한다
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(
                student,
                templates.stream().filter(TrainingCatalogPolicy::isSelectable).toList()
        );
        ReflectionTestUtils.setField(
                curriculum,
                "id",
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        );
        for (int index = 0; index < curriculum.getTrainings().size(); index++) {
            ReflectionTestUtils.setField(
                    curriculum.getTrainings().get(index),
                    "id",
                    100L + index
            );
        }
        when(curriculumRepository.findForGeneration(
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(templateRepository.findCanonicalCatalog(1L, 34L)).thenReturn(templates);
        when(dataRepository.findByTrainingId(any()))
                .thenReturn(Optional.of(mock(TrainingDataEntity.class)));

        DemoAllTrainingCurriculumInitializer initializer =
                new DemoAllTrainingCurriculumInitializer(
                        curriculumRepository,
                        templateRepository,
                        dataRepository,
                        generationService,
                        objectMapper
                );

        initializer.run(mock(org.springframework.boot.ApplicationArguments.class));

        verify(generationService, times(0)).generate(any());
        verify(dataRepository, times(0)).flush();
    }

    private List<TrainingTemplateEntity> canonicalTemplates() {
        return IntStream.rangeClosed(
                        1,
                        DemoAllTrainingCurriculumInitializer.DEMO_TEMPLATE_COUNT
                )
                .mapToObj(this::template)
                .toList();
    }

    private TrainingTemplateEntity template(int id) {
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn((long) id);
        return template;
    }
}
