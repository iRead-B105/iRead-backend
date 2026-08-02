package com.iread.backend.test.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestRecommendationCoordinatorTest {
    private TestRecommendationStateService stateService;
    private TestRecommendationWorkService workService;
    private TestRecommendationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        stateService = mock(TestRecommendationStateService.class);
        workService = mock(TestRecommendationWorkService.class);
        coordinator = new TestRecommendationCoordinator(stateService, workService);
    }

    @Test
    void runsRecommendationWorkThenCompletesState() {
        when(stateService.beginAttempt(500L)).thenReturn(true);

        coordinator.process(500L);

        verify(workService).process(500L);
        verify(stateService).complete(500L);
        verify(stateService, never()).fail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void recordsFailureWithoutRollingBackCompletedTest() {
        when(stateService.beginAttempt(500L)).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("프로필 계산 실패");
        doThrow(failure).when(workService).process(500L);

        coordinator.process(500L);

        verify(stateService).fail(500L, failure);
        verify(stateService, never()).complete(500L);
    }

    @Test
    void ignoresDuplicateProcessingRequest() {
        when(stateService.beginAttempt(500L)).thenReturn(false);

        coordinator.process(500L);

        verify(workService, never()).process(500L);
    }

    @Test
    void retriesFailedRecommendationAndCompletesIt() {
        when(stateService.prepareRetry(500L)).thenReturn(true);
        when(stateService.beginAttempt(500L)).thenReturn(true);

        boolean accepted = coordinator.retry(500L);

        org.assertj.core.api.Assertions.assertThat(accepted).isTrue();
        verify(stateService).prepareRetry(500L);
        verify(workService).process(500L);
        verify(stateService).complete(500L);
    }

    @Test
    void isolatesFailureWhileStartingRecommendationProcessing() {
        when(stateService.beginAttempt(500L))
                .thenThrow(new IllegalStateException("상태 저장 실패"));

        org.assertj.core.api.Assertions.assertThatCode(() -> coordinator.process(500L))
                .doesNotThrowAnyException();

        verify(workService, never()).process(500L);
    }

    @Test
    void isolatesFailureWhileRecordingRecommendationFailure() {
        when(stateService.beginAttempt(500L)).thenReturn(true);
        doThrow(new IllegalStateException("프로필 계산 실패"))
                .when(workService).process(500L);
        doThrow(new IllegalStateException("실패 상태 저장 실패"))
                .when(stateService)
                .fail(org.mockito.ArgumentMatchers.eq(500L), org.mockito.ArgumentMatchers.any());

        org.assertj.core.api.Assertions.assertThatCode(() -> coordinator.process(500L))
                .doesNotThrowAnyException();
    }
}
