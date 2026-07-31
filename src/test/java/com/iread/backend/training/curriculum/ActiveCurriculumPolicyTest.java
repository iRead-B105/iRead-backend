package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ActiveCurriculumPolicyTest {

    private final DailyCurriculumRepository repository =
            mock(DailyCurriculumRepository.class);

    @Test
    void 진행중인_교육과정을_대기_교육과정보다_우선한다() {
        DailyCurriculumEntity inProgress = mock(DailyCurriculumEntity.class);
        when(repository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.of(inProgress));

        assertThat(ActiveCurriculumPolicy.find(repository, 20L))
                .containsSame(inProgress);
        verify(repository).findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    void 진행중인_교육과정이_없으면_대기_교육과정을_선택한다() {
        DailyCurriculumEntity notStarted = mock(DailyCurriculumEntity.class);
        when(repository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.empty());
        when(repository.findByStudentIdAndStatus(
                20L,
                DailyCurriculumStatus.NOT_STARTED
        )).thenReturn(Optional.of(notStarted));

        assertThat(ActiveCurriculumPolicy.find(repository, 20L))
                .containsSame(notStarted);
    }
}
