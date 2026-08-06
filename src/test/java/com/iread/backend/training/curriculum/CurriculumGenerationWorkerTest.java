package com.iread.backend.training.curriculum;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.training.domain.CurriculumReviewStatus;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurriculumGenerationWorkerTest {

    @Test
    void storesAndUnlocksAllFiveOnlyAfterEveryGenerationSucceeds() {
        Fixture fixture = fixture();
        when(fixture.generationService().generate(any())).thenAnswer(invocation -> {
            TrainingEntity training = invocation.getArgument(0);
            ObjectNode value = JsonMapper.builder().build().createObjectNode();
            value.put("trainingId", training.getId());
            return value;
        });
        when(fixture.trainingDataRepository().findByTrainingId(any()))
                .thenReturn(Optional.empty());
        when(fixture.trainingDataRepository().save(any(TrainingDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fixture.worker().generate(100L);
        fixture.worker().generate(100L);

        assertThat(fixture.curriculum().getTrainings())
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_STARTED);
        verify(fixture.trainingDataRepository(), org.mockito.Mockito.times(5))
                .save(any(TrainingDataEntity.class));
        verify(fixture.trainingDataRepository()).flush();
        verify(fixture.generationService(), org.mockito.Mockito.times(5))
                .generate(any(TrainingEntity.class));
    }

    @Test
    void generationFailureLeavesEveryTrainingNotReadyAndStoresNothing() {
        Fixture fixture = fixture();
        ObjectNode generated = JsonMapper.builder().build().createObjectNode();
        when(fixture.generationService().generate(any()))
                .thenReturn(generated, generated, generated, generated)
                .thenThrow(new IllegalStateException("fifth failed"));

        assertThatThrownBy(() -> fixture.worker().generate(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fifth failed");

        assertThat(fixture.curriculum().getTrainings())
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);
        verify(fixture.trainingDataRepository(), never())
                .save(any(TrainingDataEntity.class));
        verify(fixture.trainingDataRepository(), never()).flush();
    }

    @Test
    void generatedRecommendedCurriculumWaitsForTeacherReview() {
        Fixture fixture = fixture(true);
        ObjectNode generated = JsonMapper.builder().build().createObjectNode();
        generated.putArray("questions").addObject().put("questionNo", 1);
        when(fixture.generationService().generate(any()))
                .thenAnswer(invocation -> generated.deepCopy());
        when(fixture.trainingDataRepository().findByTrainingId(any()))
                .thenReturn(Optional.empty());
        when(fixture.trainingDataRepository().save(any(TrainingDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fixture.worker().generate(100L);

        assertThat(fixture.curriculum().getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.REVIEW_REQUIRED);
        assertThat(fixture.curriculum().isAvailableToStudent()).isFalse();
    }

    private Fixture fixture() {
        return fixture(false);
    }

    private Fixture fixture(boolean recommended) {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingDataRepository trainingData = mock(TrainingDataRepository.class);
        PersonalizedTrainingGenerationService generation =
                mock(PersonalizedTrainingGenerationService.class);
        CurriculumGenerationWorker worker = new CurriculumGenerationWorker(
                curricula,
                trainingData,
                generation,
                mock(com.iread.backend.learning.app.service.LearningQuestionImageAfterCommitTrigger.class),
                JsonMapper.builder().build()
        );

        StudentEntity student = mock(StudentEntity.class);
        List<TrainingTemplateEntity> templates = java.util.stream.LongStream.rangeClosed(1, 5)
                .mapToObj(id -> {
                    TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
                    when(template.getId()).thenReturn(id);
                    return template;
                })
                .toList();
        DailyCurriculumEntity curriculum = recommended
                ? new DailyCurriculumEntity(
                        student,
                        templates,
                        mock(TestCurriculumEntity.class)
                )
                : new DailyCurriculumEntity(student, templates);
        ReflectionTestUtils.setField(curriculum, "id", 100L);
        for (int index = 0; index < curriculum.getTrainings().size(); index++) {
            ReflectionTestUtils.setField(
                    curriculum.getTrainings().get(index), "id", (long) index + 1
            );
        }
        when(curricula.findForGeneration(100L)).thenReturn(Optional.of(curriculum));
        return new Fixture(worker, curriculum, trainingData, generation);
    }

    private record Fixture(
            CurriculumGenerationWorker worker,
            DailyCurriculumEntity curriculum,
            TrainingDataRepository trainingDataRepository,
            PersonalizedTrainingGenerationService generationService
    ) {
    }
}
