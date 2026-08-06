package com.iread.backend.learning.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 교안·검사 문항 생성이 커밋되면 백그라운드에서 그림 문항의 삽화를 채운다.
 * 이미지 생성(장당 수 초~수십 초)을 생성 트랜잭션 안에서 돌리면 데모 치트의
 * 동기 경로와 실력검증 시작이 그만큼 막히므로 반드시 커밋 후 비동기로 돌린다.
 */
@Slf4j
@Component
public class LearningQuestionImageAfterCommitTrigger {

    private final TaskExecutor taskExecutor;
    private final LearningQuestionImagePopulator populator;

    public LearningQuestionImageAfterCommitTrigger(
            @Qualifier("trainingCompletionTaskExecutor") TaskExecutor taskExecutor,
            LearningQuestionImagePopulator populator
    ) {
        this.taskExecutor = taskExecutor;
        this.populator = populator;
    }

    public void populateTrainingsAfterCommit(List<Long> trainingIds) {
        List<Long> ids = List.copyOf(trainingIds);
        afterCommit(() -> populator.populateTrainings(ids));
    }

    public void populateTestsAfterCommit(List<Long> testIds) {
        List<Long> ids = List.copyOf(testIds);
        afterCommit(() -> populator.populateTests(ids));
    }

    private void afterCommit(Runnable work) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submit(work);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submit(work);
            }
        });
    }

    private void submit(Runnable work) {
        taskExecutor.execute(() -> {
            try {
                work.run();
            } catch (Exception exception) {
                // 삽화가 없으면 앱이 묘사 텍스트로 폴백하므로 학습은 계속된다.
                log.warn("학습 문항 삽화 채우기 실패", exception);
            }
        });
    }
}
