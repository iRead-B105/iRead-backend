package com.iread.backend.test.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.app.dto.req.*;
import com.iread.backend.test.app.dto.res.*;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTestService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TestDataRepository testDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final ObjectMapper objectMapper;

    public TestIntroResponse getIntro(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findCurrentTest(studentId, Set.of(
                TestStatus.NOT_STARTED,
                TestStatus.IN_PROGRESS
        ));
        JsonNode questions = readQuestions(test.getId());
        return new TestIntroResponse(
                test.getId(),
                studentId,
                test.getCreatedAt(),
                test.getStatus(),
                questions.size()
        );
    }

    public TestQuestionResponse getQuestion(
            Long teacherId,
            Long studentId,
            Long testId,
            int questionNumber
    ) {
        findOwnedTest(teacherId, studentId, testId);
        JsonNode questions = readQuestions(testId);
        if (questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("검사 문항을 찾을 수 없습니다.");
        }
        return new TestQuestionResponse(
                testId,
                questionNumber,
                questions.size(),
                questions.get(questionNumber - 1)
        );
    }

    @Transactional
    public TestStartResponse start(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findCurrentTest(studentId, Set.of(TestStatus.NOT_STARTED));
        LocalDateTime startedAt = LocalDateTime.now();
        test.start(startedAt);
        return new TestStartResponse(test.getId(), startedAt, test.getStatus());
    }

    @Transactional
    public TestResetResponse reset(Long teacherId, Long studentId, Long testId) {
        StudentTestEntity test = findOwnedTest(teacherId, studentId, testId);
        test.reset();
        wordAttemptLogRepository.deleteAllByTestId(testId);
        return new TestResetResponse(testId, test.getStatus(), LocalDateTime.now());
    }

    @Transactional
    public TestRecordingResponse saveRecording(
            Long teacherId,
            Long studentId,
            int questionNumber,
            TestRecordingRequest request
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findInProgressTest(studentId, request.testId());
        validateQuestion(request.testId(), questionNumber);
        WordEntity word = findWord(request.wordId());
        WordAttemptLogEntity attempt = wordAttemptLogRepository.saveAndFlush(
                WordAttemptLogEntity.forTest(
                        student,
                        word,
                        test,
                        true,
                        request.recognizedText(),
                        request.speechStartOffsetMs(),
                        request.speechEndOffsetMs(),
                        request.isCorrect(),
                        request.totalScore()
                )
        );
        return new TestRecordingResponse(
                attempt.getId(),
                test.getId(),
                word.getId(),
                attempt.getRecognizedText(),
                attempt.getTotalScore(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public TestSelectionResponse saveSelection(
            Long teacherId,
            Long studentId,
            int questionNumber,
            TestSelectionRequest request
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findInProgressTest(studentId, request.testId());
        validateQuestion(request.testId(), questionNumber);
        WordEntity word = findWord(request.wordId());
        WordAttemptLogEntity attempt = wordAttemptLogRepository.saveAndFlush(
                WordAttemptLogEntity.forTest(
                        student,
                        word,
                        test,
                        false,
                        null,
                        null,
                        null,
                        request.isCorrect(),
                        request.totalScore()
                )
        );
        return new TestSelectionResponse(
                attempt.getId(),
                test.getId(),
                word.getId(),
                attempt.getCorrect(),
                attempt.getTotalScore(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public TestQuestionCompleteResponse completeQuestion(
            Long teacherId,
            Long studentId,
            int questionNumber,
            TestQuestionCompleteRequest request
    ) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findInProgressTest(studentId, request.testId());
        validateQuestion(request.testId(), questionNumber);
        test.updateResult(writeJson(request.result()));
        return new TestQuestionCompleteResponse(
                test.getId(),
                questionNumber,
                test.getStatus(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public TestCompleteResponse complete(
            Long teacherId,
            Long studentId,
            TestCompleteRequest request
    ) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findInProgressTest(studentId, request.testId());
        List<WordAttemptLogEntity> attempts =
                wordAttemptLogRepository.findAllByTestIdOrderByIdAsc(test.getId());
        if (attempts.isEmpty()) {
            throw new ConflictException("저장된 검사 응답이 없습니다.");
        }
        long scoreSum = attempts.stream()
                .map(WordAttemptLogEntity::getTotalScore)
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal accuracy = BigDecimal.valueOf(scoreSum)
                .divide(BigDecimal.valueOf(attempts.size() * 10L), 2, RoundingMode.HALF_UP);
        String result = test.getResult() != null
                ? test.getResult()
                : writeJson(Map.of("attemptCount", attempts.size()));
        test.complete(result, accuracy, request.completedAt());
        return new TestCompleteResponse(
                test.getId(),
                test.getStatus(),
                test.getAccuracy(),
                test.getFinishedAt()
        );
    }

    private StudentEntity findOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private StudentTestEntity findOwnedTest(Long teacherId, Long studentId, Long testId) {
        findOwnedStudent(teacherId, studentId);
        return testRepository.findByIdAndTestCurriculumStudentId(testId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("검사를 찾을 수 없습니다."));
    }

    private StudentTestEntity findCurrentTest(Long studentId, Set<TestStatus> statuses) {
        return testRepository
                .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                        studentId,
                        statuses
                )
                .orElseThrow(() -> new ResourceNotFoundException("진행할 검사를 찾을 수 없습니다."));
    }

    private StudentTestEntity findInProgressTest(Long studentId, Long testId) {
        StudentTestEntity test = testRepository
                .findByIdAndTestCurriculumStudentId(testId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("검사를 찾을 수 없습니다."));
        if (test.getStatus() != TestStatus.IN_PROGRESS) {
            throw new ConflictException("진행 중인 검사가 아닙니다.");
        }
        return test;
    }

    private WordEntity findWord(Long wordId) {
        return wordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("단어를 찾을 수 없습니다."));
    }

    private void validateQuestion(Long testId, int questionNumber) {
        JsonNode questions = readQuestions(testId);
        if (questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("검사 문항을 찾을 수 없습니다.");
        }
    }

    private JsonNode readQuestions(Long testId) {
        JsonNode generatedData = testDataRepository
                .findFirstByTestIdOrderByCreatedAtDescIdDesc(testId)
                .map(TestDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("검사 문항을 찾을 수 없습니다."));
        JsonNode questions = generatedData.path("questions");
        if (!questions.isArray()) {
            throw new IllegalStateException("저장된 검사 문항 형식이 올바르지 않습니다.");
        }
        return questions;
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 검사 문항을 읽을 수 없습니다.", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("검사 결과를 저장할 수 없습니다.", exception);
        }
    }
}
