package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurriculumGenerationSchedulerTest {

    @Test
    void oneCurriculumFailureDoesNotPreventTheNextCurriculum() {
        DailyCurriculumRepository repository = mock(DailyCurriculumRepository.class);
        CurriculumGenerationWorker worker = mock(CurriculumGenerationWorker.class);
        DailyCurriculumEntity first = curriculum(1L, TrainingStatus.NOT_READY);
        DailyCurriculumEntity second = curriculum(2L, TrainingStatus.NOT_STARTED);
        when(repository.findAllByStatus(DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("failed")).when(worker).generate(1L);

        new CurriculumGenerationScheduler(repository, worker)
                .generateScheduledCurricula();

        verify(worker).generate(1L);
        verify(worker).generate(2L);
    }

    @Test
    void skipsMalformedMixedAndAllTypeDemoCurriculaWithoutCallingWorker() {
        DailyCurriculumRepository repository = mock(DailyCurriculumRepository.class);
        CurriculumGenerationWorker worker = mock(CurriculumGenerationWorker.class);
        DailyCurriculumEntity fourTrainings = curriculumWithCount(
                3L,
                4,
                TrainingStatus.NOT_READY
        );
        DailyCurriculumEntity mixed = curriculumWithMixedStatuses(4L);
        DailyCurriculumEntity allTypes = curriculumWithCount(
                5L,
                34,
                TrainingStatus.NOT_READY
        );
        when(repository.findAllByStatus(DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(List.of(fourTrainings, mixed, allTypes));

        new CurriculumGenerationScheduler(repository, worker)
                .generateScheduledCurricula();

        verifyNoInteractions(worker);
    }

    private DailyCurriculumEntity curriculum(long id, TrainingStatus status) {
        return curriculumWithCount(
                id,
                PersonalizedCurriculumPlanner.TRAINING_COUNT,
                status
        );
    }

    private DailyCurriculumEntity curriculumWithCount(
            long id,
            int count,
            TrainingStatus status
    ) {
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        when(curriculum.getId()).thenReturn(id);
        List<TrainingEntity> trainings = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> training(status))
                .toList();
        when(curriculum.getTrainings()).thenReturn(trainings);
        return curriculum;
    }

    private DailyCurriculumEntity curriculumWithMixedStatuses(long id) {
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        when(curriculum.getId()).thenReturn(id);
        List<TrainingEntity> trainings = List.of(
                training(TrainingStatus.NOT_STARTED),
                training(TrainingStatus.NOT_READY),
                training(TrainingStatus.NOT_READY),
                training(TrainingStatus.NOT_READY),
                training(TrainingStatus.NOT_READY)
        );
        when(curriculum.getTrainings()).thenReturn(trainings);
        return curriculum;
    }

    private TrainingEntity training(TrainingStatus status) {
        TrainingEntity training = mock(TrainingEntity.class);
        when(training.getStatus()).thenReturn(status);
        return training;
    }
}
