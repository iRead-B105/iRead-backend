package com.iread.backend.training.config;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Component
@Order(50)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-personalized-curriculum.enabled",
        havingValue = "true"
)
public class DemoAllTrainingCurriculumInitializer implements ApplicationRunner {

    static final long DEMO_CURRICULUM_ID = 190001L;

    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final PersonalizedCurriculumPlanner curriculumPlanner;
    private final TrainingDataRepository trainingDataRepository;
    private final PersonalizedTrainingGenerationService generationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findForGeneration(DEMO_CURRICULUM_ID)
                .orElse(null);
        if (curriculum == null) {
            return;
        }
        if (isAlreadyInitialized(curriculum)) {
            refreshQuestions(curriculum);
            return;
        }

        for (TrainingEntity training : curriculum.getTrainings()) {
            trainingDataRepository.deleteByTrainingId(training.getId());
        }
        trainingDataRepository.flush();

        curriculum.replaceTrainings(List.of());
        dailyCurriculumRepository.flush();

        List<TrainingTemplateEntity> templates =
                curriculumPlanner.selectTemplates(curriculum.getStudent().getId());

        curriculum.replaceTrainings(templates);
        dailyCurriculumRepository.flush();

        List<TrainingEntity> trainings = curriculum.getTrainings();
        for (TrainingEntity training : trainings) {
            ObjectNode generated = generationService.generate(training);
            trainingDataRepository.save(
                    new TrainingDataEntity(training, writeJson(generated))
            );
        }
        trainings.getFirst().markReady();
        trainingDataRepository.flush();
    }

    private void refreshQuestions(DailyCurriculumEntity curriculum) {
        for (TrainingEntity training : curriculum.getTrainings()) {
            TrainingDataEntity data = trainingDataRepository.findByTrainingId(training.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "데모 맞춤 커리큘럼 문항 데이터가 없습니다: " + training.getId()
                    ));
            ObjectNode generated = generationService.generate(training);
            data.updateGeneratedData(writeJson(generated));
        }
        trainingDataRepository.flush();
    }

    private boolean isAlreadyInitialized(DailyCurriculumEntity curriculum) {
        if (curriculum.getTrainings().size() != PersonalizedCurriculumPlanner.TRAINING_COUNT) {
            return false;
        }
        return curriculum.getTrainings().stream()
                .allMatch(training -> trainingDataRepository
                        .findByTrainingId(training.getId())
                        .isPresent());
    }


    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("데모 맞춤 커리큘럼 문항을 저장하지 못했습니다.", exception);
        }
    }
}
