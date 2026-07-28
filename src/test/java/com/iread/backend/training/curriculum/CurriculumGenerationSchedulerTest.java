package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurriculumGenerationSchedulerTest {

    @Test
    void oneCurriculumFailureDoesNotPreventTheNextCurriculum() {
        DailyCurriculumRepository repository = mock(DailyCurriculumRepository.class);
        CurriculumGenerationWorker worker = mock(CurriculumGenerationWorker.class);
        DailyCurriculumEntity first = mock(DailyCurriculumEntity.class);
        DailyCurriculumEntity second = mock(DailyCurriculumEntity.class);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        when(repository.findAllByStatus(DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("failed")).when(worker).generate(1L);

        new CurriculumGenerationScheduler(repository, worker)
                .generateScheduledCurricula();

        verify(worker).generate(1L);
        verify(worker).generate(2L);
    }
}
