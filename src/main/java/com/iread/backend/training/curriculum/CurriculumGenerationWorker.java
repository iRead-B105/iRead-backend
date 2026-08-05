package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumGenerationWorker {

    private final DailyCurriculumRepository curriculumRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final PersonalizedTrainingGenerationService generationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void generate(Long curriculumId) {
        DailyCurriculumEntity curriculum = curriculumRepository.findForGeneration(curriculumId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "일일 커리큘럼을 찾을 수 없습니다: " + curriculumId
                ));
        if (curriculum.getStatus() != DailyCurriculumStatus.NOT_STARTED) {
            return;
        }
        List<TrainingEntity> trainings = curriculum.getTrainings();
        if (trainings.size() != PersonalizedCurriculumPlanner.TRAINING_COUNT) {
            throw new IllegalStateException("맞춤 커리큘럼은 훈련 5개여야 합니다.");
        }
        if (trainings.stream().allMatch(training ->
                training.getStatus() == TrainingStatus.NOT_STARTED)) {
            curriculum.refreshReviewRequirement();
            return;
        }
        if (trainings.stream().anyMatch(training ->
                training.getStatus() != TrainingStatus.NOT_READY)) {
            throw new IllegalStateException(
                    "일부만 생성된 커리큘럼은 자동 생성할 수 없습니다: " + curriculumId
            );
        }

        List<ObjectNode> generated = new ArrayList<>();
        for (TrainingEntity training : trainings) {
            generated.add(generationService.generate(training));
        }

        for (int index = 0; index < trainings.size(); index++) {
            TrainingEntity training = trainings.get(index);
            TrainingDataEntity data = trainingDataRepository.findByTrainingId(training.getId())
                    .orElseGet(() -> trainingDataRepository.save(
                            new TrainingDataEntity(training, "{}")
                    ));
            ObjectNode previous = readObject(data.getGeneratedData());
            ObjectNode next = generated.get(index);
            next.put("revision", Math.max(0, previous.path("revision").asInt(0)) + 1);
            data.updateGeneratedData(writeJson(next));
            training.markReady();
        }
        curriculum.refreshReviewRequirement();
        trainingDataRepository.flush();
    }

    private ObjectNode readObject(String value) {
        try {
            var parsed = objectMapper.readTree(value == null || value.isBlank() ? "{}" : value);
            if (parsed instanceof ObjectNode object) {
                return object;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalStateException("Stored training data is not a JSON object.");
    }

    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("생성 훈련 JSON 저장에 실패했습니다.", exception);
        }
    }
}
