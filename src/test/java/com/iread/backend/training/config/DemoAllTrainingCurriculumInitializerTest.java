package com.iread.backend.training.config;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
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
    void createsFivePersonalizedTrainingsWithFiveQuestionsAndUnlocksOnlyTheFirst() throws Exception {
        DailyCurriculumRepository curriculumRepository =
                mock(DailyCurriculumRepository.class);
        PersonalizedCurriculumPlanner curriculumPlanner =
                mock(PersonalizedCurriculumPlanner.class);
        TrainingDataRepository dataRepository =
                mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generationService =
                mock(PersonalizedTrainingGenerationService.class);
        ObjectMapper objectMapper = JsonMapper.builder().build();

        StudentEntity student = StudentEntity.builder().name("샛별").build();
        ReflectionTestUtils.setField(student, "id", 2001L);
        List<TrainingTemplateEntity> previousTemplates = IntStream.rangeClosed(1, 2)
                .mapToObj(index -> mock(TrainingTemplateEntity.class))
                .toList();
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(student, previousTemplates);
        ReflectionTestUtils.setField(
                curriculum,
                "id",
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        );
        ReflectionTestUtils.setField(curriculum.getTrainings().get(0), "id", 91L);
        ReflectionTestUtils.setField(curriculum.getTrainings().get(1), "id", 92L);
        List<TrainingTemplateEntity> templates = IntStream
                .rangeClosed(1, PersonalizedCurriculumPlanner.TRAINING_COUNT)
                .mapToObj(index -> mock(TrainingTemplateEntity.class))
                .toList();

        when(curriculumRepository.findForGeneration(
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(curriculumPlanner.selectTemplates(2001L)).thenReturn(templates);
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
                        curriculumPlanner,
                        dataRepository,
                        generationService,
                        objectMapper
                );

        initializer.run(mock(org.springframework.boot.ApplicationArguments.class));

        assertThat(curriculum.getTrainings())
                .hasSize(PersonalizedCurriculumPlanner.TRAINING_COUNT);
        assertThat(curriculum.getTrainings().getFirst().getStatus())
                .isEqualTo(TrainingStatus.NOT_STARTED);
        assertThat(curriculum.getTrainings().subList(
                1,
                PersonalizedCurriculumPlanner.TRAINING_COUNT
        ))
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);

        ArgumentCaptor<TrainingDataEntity> captor =
                ArgumentCaptor.forClass(TrainingDataEntity.class);
        verify(
                dataRepository,
                times(PersonalizedCurriculumPlanner.TRAINING_COUNT)
        ).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(data -> {
            try {
                assertThat(objectMapper.readTree(data.getGeneratedData()).path("questions"))
                        .hasSize(5);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        verify(curriculumRepository, times(2)).flush();
        });
        verify(dataRepository, times(2)).flush();
        verify(dataRepository).deleteByTrainingId(91L);
        verify(dataRepository).deleteByTrainingId(92L);
    }

    @Test
    void refreshesFiveQuestionsForEveryExistingDemoTraining() throws Exception {
        DailyCurriculumRepository curriculumRepository =
                mock(DailyCurriculumRepository.class);
        PersonalizedCurriculumPlanner curriculumPlanner =
                mock(PersonalizedCurriculumPlanner.class);
        TrainingDataRepository dataRepository =
                mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generationService =
                mock(PersonalizedTrainingGenerationService.class);
        ObjectMapper objectMapper = JsonMapper.builder().build();

        StudentEntity student = StudentEntity.builder().name("샛별").build();
        ReflectionTestUtils.setField(student, "id", 2001L);
        List<TrainingTemplateEntity> templates = IntStream.rangeClosed(
                        1,
                        PersonalizedCurriculumPlanner.TRAINING_COUNT
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
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
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
                DemoAllTrainingCurriculumInitializer.DEMO_CURRICULUM_ID
        )).thenReturn(Optional.of(curriculum));
        when(dataRepository.findByTrainingId(any())).thenAnswer(invocation ->
                Optional.ofNullable(dataByTrainingId.get(invocation.getArgument(0)))
        );
        when(generationService.generate(any())).thenAnswer(invocation -> {
            ObjectNode generated = objectMapper.createObjectNode();
            IntStream.rangeClosed(1, 5).forEach(questionNo -> generated.withArray("questions")
                    .addObject().put("questionNo", questionNo).put("type", "REFRESHED"));
            return generated;
        });

        DemoAllTrainingCurriculumInitializer initializer =
                new DemoAllTrainingCurriculumInitializer(
                        curriculumRepository,
                        curriculumPlanner,
                        dataRepository,
                        generationService,
                        objectMapper
                );

        initializer.run(mock(org.springframework.boot.ApplicationArguments.class));

        assertThat(dataByTrainingId.values()).allSatisfy(refreshed -> {
            try {
                assertThat(objectMapper.readTree(refreshed.getGeneratedData()).path("questions"))
                        .hasSize(5);
                assertThat(objectMapper.readTree(refreshed.getGeneratedData())
                        .path("questions").get(0).path("type").asText())
                        .isEqualTo("REFRESHED");
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        verify(
                generationService,
                times(PersonalizedCurriculumPlanner.TRAINING_COUNT)
        ).generate(any());
        verify(dataRepository).flush();
    }
}
