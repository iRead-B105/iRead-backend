package com.iread.backend.training.input;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrainingInputRequirementService {

    private final TrainingDataRepository trainingDataRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final ObjectMapper objectMapper;

    public void requireQuestionInput(
            Long trainingId,
            int questionNumber,
            TrainingInputType inputType
    ) {
        if (!inputsForQuestion(trainingId, questionNumber).contains(inputType)) {
            throw new ConflictException(
                    "이 훈련 문항은 " + inputType + " 입력을 사용하지 않습니다."
            );
        }
    }

    public Set<TrainingInputType> inputsForQuestion(
            Long trainingId,
            int questionNumber
    ) {
        return TrainingInputPolicy.forQuestion(
                findQuestion(trainingId, questionNumber)
        );
    }

    public void requireTrainingInput(Long trainingId, TrainingInputType inputType) {
        boolean required = questions(trainingId).stream()
                .anyMatch(question ->
                        TrainingInputPolicy.forQuestion(question).contains(inputType));
        if (!required) {
            throw new ConflictException(
                    "이 훈련은 " + inputType + " 입력을 사용하지 않습니다."
            );
        }
    }

    public void validateCompletion(Long trainingId) {
        List<Integer> missingVoiceQuestions = new ArrayList<>();
        boolean gazeRequired = false;
        for (JsonNode question : questions(trainingId)) {
            int questionNo = question.path("questionNo").asInt(-1);
            if (questionNo < 1) {
                throw new IllegalStateException("훈련 문항의 questionNo가 올바르지 않습니다.");
            }
            var requiredInputs = TrainingInputPolicy.forQuestion(question);
            if (requiredInputs.contains(TrainingInputType.VOICE)
                    && !wordAttemptLogRepository
                    .existsByTrainingIdAndQuestionNoAndFinalAttemptTrueAndHasAudioDataTrueAndPronunciationAccuracyScoreIsNotNull(
                            trainingId,
                            questionNo
                    )) {
                missingVoiceQuestions.add(questionNo);
            }
            gazeRequired |= requiredInputs.contains(TrainingInputType.GAZE);
        }

        if (!missingVoiceQuestions.isEmpty()) {
            throw new ConflictException(
                    "최종 녹음이 필요한 훈련 문항이 남아 있습니다: " + missingVoiceQuestions
            );
        }
        if (gazeRequired && !gazeSessionRepository
                .existsByTrainingIdAndStatusAndDataUrlIsNotNull(
                        trainingId,
                        GazeSessionStatus.COMPLETED
                )) {
            throw new ConflictException("완료된 시선 입력이 필요합니다.");
        }
        if (gazeRequired && wordAttemptLogRepository
                .findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(trainingId)
                .stream()
                .anyMatch(attempt -> attempt.getTotalScore() == null)) {
            throw new ConflictException(
                    "단어별 시선 지표가 모두 연결된 후 훈련을 완료할 수 있습니다."
            );
        }
    }

    private JsonNode findQuestion(Long trainingId, int questionNumber) {
        for (JsonNode question : questions(trainingId)) {
            if (question.path("questionNo").asInt(-1) == questionNumber) {
                return question;
            }
        }
        throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
    }

    private List<JsonNode> questions(Long trainingId) {
        JsonNode generated = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "훈련 문항을 찾을 수 없습니다."
                ));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        List<JsonNode> result = new ArrayList<>();
        questions.forEach(result::add);
        return List.copyOf(result);
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 훈련 문항을 읽을 수 없습니다.", exception);
        }
    }
}
