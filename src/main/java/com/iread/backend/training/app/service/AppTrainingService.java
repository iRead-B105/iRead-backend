package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.dto.req.CompleteTrainingRequest;
import com.iread.backend.training.admin.service.TrainingService;
import com.iread.backend.training.app.dto.req.TrainingRecordingRequest;
import com.iread.backend.training.app.dto.req.TrainingSelectionRequest;
import com.iread.backend.training.app.dto.res.*;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTrainingService {
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    private final TrainingService trainingService;
    private final ObjectMapper objectMapper;

    public TrainingIntroResponse getIntro(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        JsonNode generatedData = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElse(null);
        return new TrainingIntroResponse(
                training.getId(),
                training.getTrainingTemplate().getId(),
                training.getDailyCurriculum().getId(),
                training.getSequenceNo(),
                training.getStatus(),
                training.getTrainingTemplate().getName(),
                studentGeneratedData(generatedData),
                training.getStartedAt(),
                training.getFinishedAt()
        );
    }

    public TrainingQuestionResponse getQuestion(
            Long teacherId,
            Long studentId,
            Long trainingId,
            int questionNumber
    ) {
        findOwnedTraining(teacherId, studentId, trainingId);
        JsonNode generatedData = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다."));
        JsonNode questions = generatedData.path("questions");
        if (!questions.isArray() || questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        return new TrainingQuestionResponse(
                trainingId,
                questionNumber,
                questions.size(),
                studentQuestion(questions.get(questionNumber - 1))
        );
    }

    @Transactional
    public TrainingStartResponse start(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        if (training.getStatus() != TrainingStatus.NOT_STARTED) {
            throw new ConflictException("시작 가능한 훈련이 아닙니다.");
        }
        LocalDateTime startedAt = LocalDateTime.now();
        training.start(startedAt);
        return new TrainingStartResponse(trainingId, startedAt, training.getStatus());
    }

    @Transactional
    public TrainingResetResponse reset(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        training.reset();
        wordAttemptLogRepository.deleteAllByTrainingId(trainingId);
        return new TrainingResetResponse(trainingId, training.getStatus(), LocalDateTime.now());
    }

    @Transactional
    public TrainingRecordingResponse saveRecording(
            Long teacherId,
            Long studentId,
            Long trainingId,
            int questionNumber,
            TrainingRecordingRequest request
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        TrainingEntity training = findInProgressTraining(studentId, trainingId);
        WordEntity word = findWord(request.wordId());
        validateRecordingTarget(
                trainingId,
                questionNumber,
                request.targetIndex(),
                request.tokenIndex(),
                request.expectedText()
        );
        if (!word.getContent().equals(request.expectedText())) {
            throw new IllegalArgumentException("wordId와 expectedText가 일치하지 않습니다.");
        }
        validateSpeechOffsets(request.speechStartOffsetMs(), request.speechEndOffsetMs());
        PronunciationAnalysisResult analysis = pronunciationAnalysisAdapter.analyze(
                new PronunciationAnalysisRequest(
                        "training-recording-" + trainingId + "-" + questionNumber
                                + "-" + request.targetIndex() + "-" + System.nanoTime(),
                        request.expectedText(),
                        request.audioFile().getOriginalFilename(),
                        audioBytes(request)
                )
        );
        int totalScore = (int) Math.round(analysis.pronunciationScore() * 10);
        boolean correct = analysis.pronunciationScore() >= 70
                && "NONE".equals(analysis.errorType());
        WordAttemptLogEntity attempt = wordAttemptLogRepository.saveAndFlush(
                new WordAttemptLogEntity(
                        student,
                        word,
                        training,
                        word.getContent(),
                        false,
                        true,
                        null,
                        null,
                        null,
                        null,
                        false,
                        0,
                        analysis.recognizedText(),
                        request.speechStartOffsetMs(),
                        request.speechEndOffsetMs(),
                        correct,
                        totalScore
                )
        );
        recordAttemptLink(training, questionNumber, request, attempt, analysis);
        return new TrainingRecordingResponse(
                attempt.getId(),
                trainingId,
                word.getId(),
                analysis.recognizedText(),
                analysis.observedPronunciation(),
                analysis.pronunciationScore(),
                analysis.confidence(),
                analysis.errorType(),
                attempt.getTotalScore(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public TrainingSelectionResponse saveSelection(
            Long teacherId,
            Long studentId,
            Long trainingId,
            TrainingSelectionRequest request
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        TrainingEntity training = findInProgressTraining(studentId, trainingId);
        WordEntity word = findWord(request.wordId());
        WordAttemptLogEntity attempt = wordAttemptLogRepository.saveAndFlush(
                new WordAttemptLogEntity(
                        student,
                        word,
                        training,
                        word.getContent(),
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        false,
                        0,
                        null,
                        null,
                        null,
                        request.isCorrect(),
                        request.totalScore()
                )
        );
        return new TrainingSelectionResponse(
                attempt.getId(),
                trainingId,
                word.getId(),
                attempt.getCorrect(),
                attempt.getTotalScore(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public TrainingCompleteResponse complete(
            Long teacherId,
            Long studentId,
            Long trainingId,
            CompleteTrainingRequest request
    ) {
        var accuracy = trainingService.completeTraining(
                teacherId,
                studentId,
                trainingId,
                request.result(),
                request.completedAt()
        );
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        return new TrainingCompleteResponse(
                trainingId,
                training.getStatus(),
                accuracy.movePointRight(1).intValue(),
                training.getFinishedAt()
        );
    }

    private StudentEntity findOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private TrainingEntity findOwnedTraining(Long teacherId, Long studentId, Long trainingId) {
        findOwnedStudent(teacherId, studentId);
        return trainingRepository.findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
    }

    private TrainingEntity findInProgressTraining(Long studentId, Long trainingId) {
        TrainingEntity training = trainingRepository
                .findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
        if (training.getStatus() != TrainingStatus.IN_PROGRESS) {
            throw new ConflictException("진행 중인 훈련이 아닙니다.");
        }
        return training;
    }

    private WordEntity findWord(Long wordId) {
        return wordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("단어를 찾을 수 없습니다."));
    }

    private void validateRecordingTarget(
            Long trainingId,
            int questionNumber,
            int targetIndex,
            Integer tokenIndex,
            String expectedText
    ) {
        JsonNode generated = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다."));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray() || questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        JsonNode question = questions.get(questionNumber - 1);
        JsonNode expected = tokenIndex == null
                ? question.path("analysisTargets").path(targetIndex)
                : question.path("words").path(tokenIndex);
        if (!expected.isObject()) {
            throw new IllegalArgumentException("요청한 분석 대상 위치가 올바르지 않습니다.");
        }
        String storedText = tokenIndex == null
                ? expected.path("text").asText()
                : expected.path("surface").asText();
        if (!expectedText.equals(storedText)) {
            throw new IllegalArgumentException("요청한 텍스트가 생성된 훈련 문항과 일치하지 않습니다.");
        }
    }

    private void recordAttemptLink(
            TrainingEntity training,
            int questionNumber,
            TrainingRecordingRequest request,
            WordAttemptLogEntity attempt,
            PronunciationAnalysisResult analysis
    ) {
        ObjectNode result = readObjectOrNew(training.getResult());
        result.put("schemaVersion", 2);
        ArrayNode attempts = result.withArray("wordAttempts");
        attempts.forEach(existing -> {
            if (existing.path("questionNo").asInt() == questionNumber
                    && existing.path("targetIndex").asInt() == request.targetIndex()
                    && java.util.Objects.equals(nullableInt(existing, "tokenIndex"), request.tokenIndex())) {
                ((ObjectNode) existing).put("isFinal", false);
            }
        });
        ObjectNode link = attempts.addObject();
        link.put("wordAttemptLogId", attempt.getId());
        link.put("questionNo", questionNumber);
        link.put("targetIndex", request.targetIndex());
        if (request.tokenIndex() == null) link.putNull("tokenIndex");
        else link.put("tokenIndex", request.tokenIndex());
        link.put("isFinal", true);
        link.put("expectedText", request.expectedText());
        link.put("observedPronunciation", analysis.observedPronunciation());
        link.put("pronunciationScore", analysis.pronunciationScore());
        link.put("pronunciationConfidence", analysis.confidence());
        link.put("pronunciationErrorType", analysis.errorType());
        link.put("pronunciationAnalysisVersion", analysis.analysisVersion());
        if (request.speechStartOffsetMs() != null && request.speechEndOffsetMs() != null) {
            link.put("wordReadTimeMs",
                    request.speechEndOffsetMs() - request.speechStartOffsetMs());
        } else {
            link.putNull("wordReadTimeMs");
        }
        training.recordProgressResult(writeJson(result));
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asInt() : null;
    }

    private ObjectNode readObjectOrNew(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        JsonNode parsed = readJson(value);
        if (parsed instanceof ObjectNode object) {
            return object.deepCopy();
        }
        throw new IllegalStateException("저장된 훈련 수행 결과가 JSON 객체가 아닙니다.");
    }

    private byte[] audioBytes(TrainingRecordingRequest request) {
        try {
            if (request.audioFile().isEmpty()) {
                throw new IllegalArgumentException("audioFile은 비어 있을 수 없습니다.");
            }
            return request.audioFile().getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("audioFile을 읽을 수 없습니다.", exception);
        }
    }

    private void validateSpeechOffsets(Integer start, Integer end) {
        if (start != null && end != null && end <= start) {
            throw new IllegalArgumentException("speechEndOffsetMs는 시작 시점보다 커야 합니다.");
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 훈련 문항을 읽을 수 없습니다.", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 수행 결과를 저장할 수 없습니다.", exception);
        }
    }

    private JsonNode studentGeneratedData(JsonNode generatedData) {
        if (!(generatedData instanceof ObjectNode root)) {
            return generatedData;
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("schemaVersion", root.path("schemaVersion").deepCopy());
        ArrayNode questions = result.putArray("questions");
        root.path("questions").forEach(question -> questions.add(studentQuestion(question)));
        return result;
    }

    private JsonNode studentQuestion(JsonNode question) {
        if (!(question instanceof ObjectNode)) {
            return question.deepCopy();
        }
        ObjectNode result = objectMapper.createObjectNode();
        for (String field : java.util.List.of("questionNo", "type", "content", "text")) {
            if (question.has(field)) {
                result.set(field, question.get(field).deepCopy());
            }
        }
        return result;
    }
}
