package com.iread.backend.training.completion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class TrainingCompletionAfterCommitPublisher {
    private final TaskExecutor taskExecutor;
    private final TrainingCompletionFollowUpWorker worker;

    public TrainingCompletionAfterCommitPublisher(
            @Qualifier("trainingCompletionTaskExecutor") TaskExecutor taskExecutor,
            TrainingCompletionFollowUpWorker worker
    ) {
        this.taskExecutor = taskExecutor;
        this.worker = worker;
    }

    public void processAfterCommit(Long studentId, boolean createNextCurriculum) {
        Runnable submit = () -> taskExecutor.execute(() -> {
            try {
                worker.process(studentId, createNextCurriculum);
            } catch (RuntimeException failure) {
                log.error(
                        "Training completion follow-up failed. studentId={}",
                        studentId,
                        failure
                );
            }
        });

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            submit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submit.run();
                    }
                }
        );
    }
}
