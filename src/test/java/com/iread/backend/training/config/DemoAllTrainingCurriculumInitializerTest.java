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
import java.util.Optional;
import java.util.stream.IntStream;

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

        assertThat(curriculum.getTrainings()).hasSize(34);
        assertThat(curriculum.getTrainings().getFirst().getStatus())
                .isEqualTo(TrainingStatus.NOT_STARTED);
        assertThat(curriculum.getTrainings().subList(1, 34))
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);

        ArgumentCaptor<TrainingDataEntity> captor =
                ArgumentCaptor.forClass(TrainingDataEntity.class);
        verify(dataRepository, times(34)).save(captor.capture());
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
}
