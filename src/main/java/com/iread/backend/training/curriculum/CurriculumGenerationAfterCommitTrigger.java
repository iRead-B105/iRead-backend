package com.iread.backend.training.curriculum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 커리큘럼이 생성·수정되면 커밋 직후 백그라운드에서 훈련 교안(문항)을 즉시 생성한다.
 *
 * 교안 생성을 새벽 배치(CurriculumGenerationScheduler)에만 맡기면 교수자가
 * 커리큘럼을 만든 뒤 훈련마다 재생성 버튼을 눌러야 하고, 아동은 다음 배치까지
 * 학습을 시작할 수 없다. 배치는 이 트리거가 실패한 건의 재시도 역할로 남는다.
 */
@Slf4j
@Component
public class CurriculumGenerationAfterCommitTrigger {

    private final TaskExecutor taskExecutor;
    private final CurriculumGenerationWorker worker;

    public CurriculumGenerationAfterCommitTrigger(
            @Qualifier("trainingCompletionTaskExecutor") TaskExecutor taskExecutor,
            CurriculumGenerationWorker worker
    ) {
        this.taskExecutor = taskExecutor;
        this.worker = worker;
    }

    public void generateAfterCommit(Long curriculumId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submit(curriculumId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submit(curriculumId);
            }
        });
    }

    private void submit(Long curriculumId) {
        taskExecutor.execute(() -> {
            try {
                worker.generate(curriculumId);
            } catch (Exception exception) {
                // 실패해도 새벽 배치가 다시 시도하고, 교수자 재생성 버튼도 남아 있다.
                log.warn("커리큘럼 {} 교안 즉시 생성 실패 (배치에서 재시도)", curriculumId, exception);
            }
        });
    }
}
