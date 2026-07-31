package com.iread.backend.realtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RealtimeEventPublisherTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void 트랜잭션이_없으면_이벤트를_즉시_발행한다() {
        RealtimeEventHub eventHub = mock(RealtimeEventHub.class);
        RealtimeEventPublisher publisher = new RealtimeEventPublisher(eventHub);

        publisher.publishAfterCommit(
                1001L,
                2001L,
                RealtimeResource.TRAINING,
                3001L,
                "COMPLETED"
        );

        verify(eventHub).publish(
                eq(1001L),
                argThat(event -> event.studentId().equals(2001L)
                        && event.resource() == RealtimeResource.TRAINING
                        && event.resourceId().equals(3001L)
                        && event.changeType().equals("COMPLETED"))
        );
    }

    @Test
    void 활성_트랜잭션에서는_커밋된_뒤에만_이벤트를_발행한다() {
        RealtimeEventHub eventHub = mock(RealtimeEventHub.class);
        RealtimeEventPublisher publisher = new RealtimeEventPublisher(eventHub);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publishAfterCommit(
                1001L,
                2001L,
                RealtimeResource.CURRICULUM,
                190001L,
                "UPDATED"
        );

        verify(eventHub, never()).publish(eq(1001L), org.mockito.ArgumentMatchers.any());
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(eventHub).publish(
                eq(1001L),
                argThat(event -> event.resource() == RealtimeResource.CURRICULUM
                        && event.resourceId().equals(190001L)
                        && event.changeType().equals("UPDATED"))
        );
    }

    @Test
    void eventVersionIsStrictlyMonotonic() {
        RealtimeEvent first = RealtimeEvent.create(
                2001L,
                RealtimeResource.TRAINING,
                3001L,
                "UPDATED"
        );
        RealtimeEvent second = RealtimeEvent.create(
                2001L,
                RealtimeResource.TRAINING,
                3001L,
                "UPDATED"
        );

        org.assertj.core.api.Assertions.assertThat(second.version())
                .isGreaterThan(first.version());
    }
}
