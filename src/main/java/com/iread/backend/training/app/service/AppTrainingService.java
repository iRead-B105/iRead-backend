package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
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
                generatedData,
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
                questions.get(questionNumber - 1)
        );
    }

    @Transactional
    public TrainingStartResponse start(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        if (training.getStatus() != TrainingStatus.NOT_STARTED) {
            throw new IllegalStateException("시작 가능한 훈련이 아닙니다.");
        }
        LocalDateTime startedAt = LocalDateTime.now();
        training.start(startedAt);
        return new TrainingStartResponse(trainingId, startedAt, training.getStatus());
    }

    @Transactional
    public TrainingResetResponse reset(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        training.reset();
        return new TrainingResetResponse(trainingId, training.getStatus(), LocalDateTime.now());
    }

    @Transactional
    public TrainingRecordingResponse saveRecording(
            Long teacherId,
            Long studentId,
            Long trainingId,
            TrainingRecordingRequest request
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
                        true,
                        null,
                        null,
                        null,
                        null,
                        false,
                        0,
                        request.recognizedText(),
                        request.speechStartOffsetMs(),
                        request.speechEndOffsetMs(),
                        request.isCorrect(),
                        request.totalScore()
                )
        );
        return new TrainingRecordingResponse(
                attempt.getId(),
                trainingId,
                word.getId(),
                attempt.getRecognizedText(),
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
            throw new IllegalStateException("진행 중인 훈련이 아닙니다.");
        }
        return training;
    }

    private WordEntity findWord(Long wordId) {
        return wordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("단어를 찾을 수 없습니다."));
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 훈련 문항을 읽을 수 없습니다.", exception);
        }
    }
}
