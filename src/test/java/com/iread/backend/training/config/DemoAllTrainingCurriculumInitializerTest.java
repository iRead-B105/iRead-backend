package com.iread.backend.training.config;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoAllTrainingCurriculumInitializerTest {

    @Test
    void createsOneQuestionForEveryTrainingTemplateAndUnlocksOnlyTheFirst() throws Exception {
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
        DailyCurriculumEntity curriculum =
                new DailyCurriculumEntity(student, List.of());
        ReflectionTestUtils.setField(
                curriculum,
                "id",
                DemoAllTrainingCurriculumInitializer.SHOWCASE_CURRICULUM_ID
        );
        List<TrainingTemplateEntity> templates = IntStream
                .rangeClosed(1, DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT)
                .mapToObj(index -> mock(TrainingTemplateEntity.class))
                .toList();

        when(curriculumRepository.findForGeneration(
                DemoAllTrainingCurriculumInitializer.SHOWCASE_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(templateRepository
                .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(templates);
        when(generationService.generate(any())).thenAnswer(invocation -> {
            ObjectNode generated = objectMapper.createObjectNode();
            generated.put("schemaVersion", 2);
            generated.putArray("questions")
                    .addObject().put("questionNo", 1).put("type", "VOWEL_SOUND_CHOICE");
            generated.withArray("questions")
                    .addObject().put("questionNo", 2).put("type", "VOWEL_SOUND_CHOICE");
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

        assertThat(curriculum.getTrainings())
                .hasSize(DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT);
        assertThat(curriculum.getTrainings().getFirst().getStatus())
                .isEqualTo(TrainingStatus.NOT_STARTED);
        assertThat(curriculum.getTrainings().subList(
                1,
                DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT
        ))
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);

        ArgumentCaptor<TrainingDataEntity> captor =
                ArgumentCaptor.forClass(TrainingDataEntity.class);
        verify(
                dataRepository,
                times(DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT)
        ).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(data -> {
            try {
                assertThat(objectMapper.readTree(data.getGeneratedData()).path("questions"))
                        .hasSize(1);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        verify(dataRepository).flush();
    }

    @Test
    void refreshesAllExistingShowcaseQuestions() throws Exception {
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
        List<TrainingTemplateEntity> templates = IntStream.rangeClosed(
                        1,
                        DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT
                )
                .mapToObj(index -> {
                    TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
                    when(template.getId()).thenReturn((long) index);
                    return template;
                })
                .toList();
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(student, templates);
        ReflectionTestUtils.setField(
                curriculum,
                "id",
                DemoAllTrainingCurriculumInitializer.SHOWCASE_CURRICULUM_ID
        );
        for (int index = 0; index < curriculum.getTrainings().size(); index++) {
            ReflectionTestUtils.setField(
                    curriculum.getTrainings().get(index),
                    "id",
                    100L + index
            );
        }
        Map<Long, TrainingDataEntity> dataByTrainingId = curriculum.getTrainings().stream()
                .collect(Collectors.toMap(
                        training -> training.getId(),
                        training -> new TrainingDataEntity(training, "{\"questions\":[]}")
                ));
        when(curriculumRepository.findForGeneration(
                DemoAllTrainingCurriculumInitializer.SHOWCASE_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(dataRepository.findByTrainingId(any())).thenAnswer(invocation ->
                Optional.ofNullable(dataByTrainingId.get(invocation.getArgument(0)))
        );
        when(generationService.generate(any())).thenAnswer(invocation -> {
            ObjectNode generated = objectMapper.createObjectNode();
            generated.putArray("questions")
                    .addObject().put("questionNo", 1).put("type", "REFRESHED");
            generated.withArray("questions")
                    .addObject().put("questionNo", 2).put("type", "REFRESHED");
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

        assertThat(dataByTrainingId.values()).allSatisfy(refreshed -> {
            try {
                assertThat(objectMapper.readTree(refreshed.getGeneratedData()).path("questions"))
                        .hasSize(1);
                assertThat(objectMapper.readTree(refreshed.getGeneratedData())
                        .path("questions").get(0).path("type").asText())
                        .isEqualTo("REFRESHED");
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        verify(
                generationService,
                times(DemoAllTrainingCurriculumInitializer.EXPECTED_TEMPLATE_COUNT)
        ).generate(any());
        verify(dataRepository).flush();
    }
}
