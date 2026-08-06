package com.iread.backend.training.curriculum;

import com.iread.backend.learning.app.service.LearningQuestionImageAfterCommitTrigger;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
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
    private final LearningQuestionImageAfterCommitTrigger imageTrigger;
    private final ObjectMapper objectMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

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
        // 이전 생성이 도중에 실패했거나 일부 훈련만 먼저 교안이 만들어진 커리큘럼도
        // 이미 준비된 훈련은 건드리지 않고 빠진 훈련만 채워 스스로 복구한다.
        List<TrainingEntity> pending = trainings.stream()
                .filter(training -> training.getStatus() == TrainingStatus.NOT_READY)
                .toList();
        if (pending.isEmpty()) {
            curriculum.refreshReviewRequirement();
            return;
        }

        List<ObjectNode> generated = new ArrayList<>();
        for (TrainingEntity training : pending) {
            generated.add(generationService.generate(training));
        }

        for (int index = 0; index < pending.size(); index++) {
            TrainingEntity training = pending.get(index);
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
        // 그림 문항 삽화는 커밋 후 백그라운드로 채운다(실패 시 앱은 묘사 텍스트 폴백).
        imageTrigger.populateTrainingsAfterCommit(
                pending.stream().map(TrainingEntity::getId).toList()
        );
        // 교사 웹이 "교안 생성 중" 버튼을 즉시 활성화할 수 있게 완료를 알린다.
        realtimeEventPublisher.publishAfterCommit(
                curriculum.getStudent().getTeacher().getId(),
                curriculum.getStudent().getId(),
                RealtimeResource.CURRICULUM,
                curriculumId,
                "MATERIALS_GENERATED"
        );
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
