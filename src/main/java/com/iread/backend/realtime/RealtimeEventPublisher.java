package com.iread.backend.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final RealtimeEventHub eventHub;

    public void publishAfterCommit(
            Long teacherId,
            Long studentId,
            RealtimeResource resource,
            Long resourceId,
            String changeType
    ) {
        Runnable publish = () -> eventHub.publish(
                teacherId,
                RealtimeEvent.create(studentId, resource, resourceId, changeType)
        );

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publish.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publish.run();
                    }
                }
        );
    }
}
