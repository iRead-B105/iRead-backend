package com.iread.backend.test.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestRecommendationAfterCommitPublisher {
    private final TestRecommendationCoordinator coordinator;

    public void processAfterCommit(Long testCurriculumId) {
        Runnable process = () -> {
            try {
                coordinator.process(testCurriculumId);
            } catch (RuntimeException failure) {
                log.error(
                        "커밋 이후 실력도전 추천 호출을 격리했습니다. testCurriculumId={}",
                        testCurriculumId,
                        failure
                );
            }
        };
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            process.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        process.run();
                    }
                }
        );
    }
}
