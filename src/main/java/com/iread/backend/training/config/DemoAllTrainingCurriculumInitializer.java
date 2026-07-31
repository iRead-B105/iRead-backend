package com.iread.backend.training.config;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Component
@Order(50)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.all-training-showcase.enabled",
        havingValue = "true"
)
public class DemoAllTrainingCurriculumInitializer implements ApplicationRunner {

    static final long SHOWCASE_CURRICULUM_ID = 190001L;
    static final int EXPECTED_TEMPLATE_COUNT = 34;

    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TrainingTemplateRepository trainingTemplateRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final PersonalizedTrainingGenerationService generationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findForGeneration(SHOWCASE_CURRICULUM_ID)
                .orElse(null);
        if (curriculum == null) {
            return;
        }
        if (isAlreadyInitialized(curriculum)) {
            refreshAllQuestions(curriculum);
            return;
        }

        List<TrainingTemplateEntity> templates =
                trainingTemplateRepository
                        .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc();
        if (templates.size() != EXPECTED_TEMPLATE_COUNT) {
            throw new IllegalStateException(
                    "전체 훈련 체험 커리큘럼에는 34개 템플릿이 필요합니다."
            );
        }

        curriculum.replaceTrainings(templates);
        dailyCurriculumRepository.flush();

        List<TrainingEntity> trainings = curriculum.getTrainings();
        for (TrainingEntity training : trainings) {
            ObjectNode generated = generationService.generate(training);
            keepFirstQuestion(generated);
            trainingDataRepository.save(
                    new TrainingDataEntity(training, writeJson(generated))
            );
        }
        trainings.getFirst().markReady();
        trainingDataRepository.flush();
    }

    private void refreshAllQuestions(DailyCurriculumEntity curriculum) {
        for (TrainingEntity training : curriculum.getTrainings()) {
            TrainingDataEntity data = trainingDataRepository.findByTrainingId(training.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "전체 훈련 체험 문항 데이터가 없습니다: " + training.getId()
                    ));
            ObjectNode generated = generationService.generate(training);
            keepFirstQuestion(generated);
            data.updateGeneratedData(writeJson(generated));
        }
        trainingDataRepository.flush();
    }

    private boolean isAlreadyInitialized(DailyCurriculumEntity curriculum) {
        if (curriculum.getTrainings().size() != EXPECTED_TEMPLATE_COUNT) {
            return false;
        }
        return curriculum.getTrainings().stream()
                .allMatch(training -> trainingDataRepository
                        .findByTrainingId(training.getId())
                        .isPresent());
    }

    private void keepFirstQuestion(ObjectNode generated) {
        ArrayNode questions = generated.withArray("questions");
        if (questions.isEmpty()) {
            throw new IllegalStateException("생성된 훈련 문항이 없습니다.");
        }
        while (questions.size() > 1) {
            questions.remove(questions.size() - 1);
        }
    }

    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("전체 훈련 체험 문항을 저장하지 못했습니다.", exception);
        }
    }
}
