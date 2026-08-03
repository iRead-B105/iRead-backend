package com.iread.backend.test.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TestRecommendationAfterCommitPublisherTest {

    @Test
    void runsRecommendationOnlyAfterTransactionCommit() {
        TestRecommendationCoordinator coordinator = mock(TestRecommendationCoordinator.class);
        TestRecommendationAfterCommitPublisher publisher =
                new TestRecommendationAfterCommitPublisher(coordinator);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.processAfterCommit(500L);

            verifyNoInteractions(coordinator);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());
            verify(coordinator).process(500L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void isolatesUnexpectedCoordinatorFailureAfterCommit() {
        TestRecommendationCoordinator coordinator = mock(TestRecommendationCoordinator.class);
        doThrow(new IllegalStateException("예상하지 못한 오류"))
                .when(coordinator)
                .process(500L);
        TestRecommendationAfterCommitPublisher publisher =
                new TestRecommendationAfterCommitPublisher(coordinator);

        org.assertj.core.api.Assertions.assertThatCode(() -> publisher.processAfterCommit(500L))
                .doesNotThrowAnyException();
    }
}
