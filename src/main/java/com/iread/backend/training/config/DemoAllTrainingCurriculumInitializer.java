package com.iread.backend.training.config;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
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
    static final int DEMO_TEMPLATE_COUNT = 31;
    private static final long FIRST_TEMPLATE_ID = 1L;
    private static final long LAST_TEMPLATE_ID = 34L;

    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TrainingTemplateRepository trainingTemplateRepository;
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
        List<TrainingTemplateEntity> templates = loadCanonicalTemplates();
        if (isAlreadyInitialized(curriculum, templates)) {
            return;
        }

        for (TrainingEntity training : curriculum.getTrainings()) {
            trainingDataRepository.deleteByTrainingId(training.getId());
        }
        trainingDataRepository.flush();

        curriculum.replaceTrainingsPreservingProgress(List.of());
        dailyCurriculumRepository.flush();

        curriculum.replaceTrainingsPreservingProgress(templates);
        dailyCurriculumRepository.flush();

        List<TrainingEntity> trainings = curriculum.getTrainings();
        for (TrainingEntity training : trainings) {
            // 데모 시드는 AI 호출 없이 항상 시드 데이터로 채운다
            ObjectNode generated = generationService.generateSeed(training);
            trainingDataRepository.save(
                    new TrainingDataEntity(training, writeJson(generated))
            );
        }
        trainings.getFirst().markReady();
        trainingDataRepository.flush();
    }

    private List<TrainingTemplateEntity> loadCanonicalTemplates() {
        List<TrainingTemplateEntity> templates = trainingTemplateRepository.findCanonicalCatalog(
                FIRST_TEMPLATE_ID,
                LAST_TEMPLATE_ID
        );
        if (templates.size() != DEMO_TEMPLATE_COUNT) {
            throw new IllegalStateException(
                    "전 유형 확인용 데모에는 사용 가능한 기준 템플릿 31개가 필요합니다."
            );
        }
        if (templates.stream().anyMatch(template -> !TrainingCatalogPolicy.isSelectable(template))) {
            throw new IllegalStateException(
                    "전 유형 확인용 데모에는 더 이상 제공하지 않는 템플릿을 포함할 수 없습니다."
            );
        }
        return templates;
    }

    private boolean isAlreadyInitialized(
            DailyCurriculumEntity curriculum,
            List<TrainingTemplateEntity> templates
    ) {
        if (curriculum.getTrainings().size() != templates.size()) {
            return false;
        }
        for (int index = 0; index < templates.size(); index++) {
            TrainingEntity training = curriculum.getTrainings().get(index);
            if (!templates.get(index).getId().equals(training.getTrainingTemplate().getId())
                    || trainingDataRepository.findByTrainingId(training.getId()).isEmpty()) {
                return false;
            }
        }
        return true;
    }


    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("데모 맞춤 커리큘럼 문항을 저장하지 못했습니다.", exception);
        }
    }
}
