package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurriculumGenerationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(CurriculumGenerationScheduler.class);

    private final DailyCurriculumRepository curriculumRepository;
    private final CurriculumGenerationWorker worker;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void generateScheduledCurricula() {
        curriculumRepository.findAllByStatus(DailyCurriculumStatus.NOT_STARTED)
                .stream()
                .filter(this::isGenerationCandidate)
                .forEach(curriculum -> {
                    try {
                        worker.generate(curriculum.getId());
                    } catch (RuntimeException exception) {
                        log.error(
                                "Curriculum generation failed. curriculumId={}",
                                curriculum.getId(),
                                exception
                        );
                    }
                });
    }

    private boolean isGenerationCandidate(DailyCurriculumEntity curriculum) {
        var trainings = curriculum.getTrainings();
        if (trainings.size() != PersonalizedCurriculumPlanner.TRAINING_COUNT) {
            return false;
        }
        boolean allNotReady = trainings.stream().allMatch(training ->
                training.getStatus() == TrainingStatus.NOT_READY
        );
        boolean allNotStarted = trainings.stream().allMatch(training ->
                training.getStatus() == TrainingStatus.NOT_STARTED
        );
        return allNotReady || allNotStarted;
    }
}
