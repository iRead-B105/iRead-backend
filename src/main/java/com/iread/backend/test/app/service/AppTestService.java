package com.iread.backend.test.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.learning.app.dto.LearningSubmission;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationWordResult;
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
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTestService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TestDataRepository testDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    private final AudioUploadPolicy audioUploadPolicy;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final ObjectMapper objectMapper;
    private final AppLearningQuestionSupport learningQuestionSupport;

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
                learningQuestionSupport.toStudentQuestion(questions.get(questionNumber - 1))
        );
    }

    @Transactional
    public TestStartResponse start(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity current = findCurrentTest(studentId, Set.of(TestStatus.NOT_STARTED));
        StudentTestEntity test = findTestForUpdate(studentId, current.getId());
        LocalDateTime startedAt = LocalDateTime.now();
        test.start(startedAt);
        return new TestStartResponse(test.getId(), startedAt, test.getStatus());
    }

    @Transactional
    public TestResetResponse reset(Long teacherId, Long studentId, Long testId) {
        StudentTestEntity test = findOwnedTestForUpdate(teacherId, studentId, testId);
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
        StudentTestEntity test = findInProgressTestForUpdate(studentId, request.testId());
        validateQuestion(request.testId(), questionNumber);
        WordEntity word = findWord(request.wordId());
        validateSpeechOffsets(request.speechStartOffsetMs(), request.speechEndOffsetMs());
        audioUploadPolicy.validate(request.audioFile());
        PronunciationAnalysisResult analysis = pronunciationAnalysisAdapter.analyze(
                new PronunciationAnalysisRequest(
                        "test-recording-" + request.testId() + "-" + questionNumber
                                + "-" + System.nanoTime(),
                        word.getContent(),
                        request.audioFile().getOriginalFilename(),
                        request.audioFile().getContentType(),
                        audioBytes(request)
                )
        );
        PronunciationWordResult wordResult = analysis.words().stream()
                .filter(result -> !result.isInsertion())
                .filter(result -> word.getContent().equals(result.word()))
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "발음 분석 결과를 검사 단어와 정렬할 수 없습니다."
                ));
        int pronunciationAccuracyScore =
                (int) Math.round(wordResult.scoreOrZero() * 10);
        boolean correct = pronunciationAccuracyScore >= 700
                && "NONE".equalsIgnoreCase(wordResult.errorType());
        int totalScore = wordAttemptScoreCalculator.calculate(
                pronunciationAccuracyScore,
                true,
                false,
                0,
                correct
        );
        markPreviousAttemptNotFinal(test.getId(), questionNumber);
        WordAttemptLogEntity attempt = wordAttemptLogRepository.saveAndFlush(
                WordAttemptLogEntity.forTest(
                        student,
                        word,
                        test,
                        true,
                        pronunciationAccuracyScore,
                        request.speechStartOffsetMs(),
                        request.speechEndOffsetMs(),
                        correct,
                        totalScore,
                        questionNumber
                )
        );
        return new TestRecordingResponse(
                attempt.getId(),
                test.getId(),
                word.getId(),
                wordResult.scoreOrZero(),
                wordResult.errorType(),
                attempt.getTotalScore(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public TestProgressResponse saveSelection(
            Long teacherId,
            Long studentId,
            int questionNumber,
            TestSubmissionRequest request
    ) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findInProgressTestForUpdate(studentId, request.testId());
        JsonNode questions = readQuestions(request.testId());
        if (questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("검사 문항을 찾을 수 없습니다.");
        }
        LearningSubmission submissionRequest = request.submission();
        ObjectNode result = readObjectOrNew(test.getResult());
        result.put("schemaVersion", 2);
        ArrayNode submissions = result.withArray("submissions");

        JsonNode existing = findSubmission(submissions, submissionRequest.submissionId());
        if (existing != null) {
            if (!sameSubmission(existing, questionNumber, submissionRequest)) {
                throw new ConflictException("같은 submissionId를 다른 제출에 재사용할 수 없습니다.");
            }
            return readProgress(existing.path("progress"));
        }
        if (hasQuestionSubmission(submissions, questionNumber)) {
            throw new ConflictException("검사 문항은 최초 제출만 인정됩니다.");
        }

        AppLearningQuestionSupport.Evaluation evaluation =
                learningQuestionSupport.evaluate(
                        questions.get(questionNumber - 1),
                        submissionRequest
                );
        ObjectNode stored = submissions.addObject();
        stored.put("submissionId", submissionRequest.submissionId().toString());
        stored.put("questionNo", questionNumber);
        stored.put("responseType", submissionRequest.responseType().name());
        stored.set("response", submissionRequest.response().deepCopy());
        stored.put("correct", evaluation.correct());
        stored.put("totalScore", evaluation.totalScore());
        stored.put("submittedAt", LocalDateTime.now().toString());

        int completedQuestions = completedQuestionNumbers(submissions).size();
        int totalQuestions = questions.size();
        Integer nextQuestion = nextQuestionNumber(submissions, totalQuestions);
        TestProgressResponse progress = new TestProgressResponse(
                "TEST_PROGRESS",
                submissionRequest.submissionId(),
                true,
                questionNumber,
                completedQuestions,
                totalQuestions,
                completedQuestions * 100 / totalQuestions,
                nextQuestion,
                completedQuestions == totalQuestions
        );
        stored.set("progress", writeProgress(progress));
        test.updateResult(writeJson(result));
        return progress;
    }

    @Transactional
    public TestCompleteResponse complete(
            Long teacherId,
            Long studentId,
            TestCompleteRequest request
    ) {
        StudentTestEntity owned = findOwnedTest(teacherId, studentId, request.testId());
        if (owned.getStatus() == TestStatus.COMPLETED) {
            return completionResponse(owned);
        }
        StudentTestEntity test = findInProgressTestForUpdate(studentId, request.testId());
        JsonNode questions = readQuestions(test.getId());
        ObjectNode result = readObjectOrNew(test.getResult());
        ArrayNode submissions = result.withArray("submissions");
        List<WordAttemptLogEntity> legacyAttempts =
                wordAttemptLogRepository.findAllByTestIdAndFinalAttemptTrueOrderByIdAsc(
                        test.getId()
                );
        int completedQuestions =
                completedQuestionNumbers(submissions).size() + legacyAttempts.size();
        if (completedQuestions != questions.size()) {
            throw new ConflictException("모든 검사 문항을 제출한 후 검사를 종료할 수 있습니다.");
        }
        long scoreSum = 0;
        for (JsonNode submission : submissions) {
            scoreSum += submission.path("totalScore").asInt();
        }
        scoreSum += legacyAttempts.stream()
                .map(WordAttemptLogEntity::getTotalScore)
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal accuracy = BigDecimal.valueOf(scoreSum)
                .divide(BigDecimal.valueOf(completedQuestions * 10L), 2, RoundingMode.HALF_UP);
        LocalDateTime completedAt = LocalDateTime.now();
        test.complete(writeJson(result), accuracy, completedAt);
        return completionResponse(test);
    }

    private TestCompleteResponse completionResponse(StudentTestEntity test) {
        return new TestCompleteResponse(
                "TEST_COMPLETED",
                test.getId(),
                test.getStatus(),
                test.getFinishedAt(),
                "TEST_COMPLETE_GREAT_JOB",
                "SHOW_COMPLETION"
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

    private StudentTestEntity findOwnedTestForUpdate(Long teacherId, Long studentId, Long testId) {
        findOwnedStudent(teacherId, studentId);
        return findTestForUpdate(studentId, testId);
    }

    private StudentTestEntity findCurrentTest(Long studentId, Set<TestStatus> statuses) {
        return testRepository
                .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                        studentId,
                        statuses
                )
                .orElseThrow(() -> new ResourceNotFoundException("진행할 검사를 찾을 수 없습니다."));
    }

    private StudentTestEntity findInProgressTestForUpdate(Long studentId, Long testId) {
        StudentTestEntity test = findTestForUpdate(studentId, testId);
        if (test.getStatus() != TestStatus.IN_PROGRESS) {
            throw new ConflictException("진행 중인 검사가 아닙니다.");
        }
        return test;
    }

    private StudentTestEntity findTestForUpdate(Long studentId, Long testId) {
        return testRepository.findByIdAndStudentIdForUpdate(testId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("검사를 찾을 수 없습니다."));
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

    private byte[] audioBytes(TestRecordingRequest request) {
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

    private void markPreviousAttemptNotFinal(Long testId, int questionNumber) {
        wordAttemptLogRepository
                .findAllByTestIdAndQuestionNoAndFinalAttemptTrue(testId, questionNumber)
                .forEach(WordAttemptLogEntity::markNotFinal);
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

    private ObjectNode readObjectOrNew(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        JsonNode parsed = readJson(value);
        if (parsed instanceof ObjectNode object) {
            return object.deepCopy();
        }
        throw new IllegalStateException("저장된 검사 결과가 JSON 객체가 아닙니다.");
    }

    private JsonNode findSubmission(ArrayNode submissions, UUID submissionId) {
        for (JsonNode submission : submissions) {
            if (submissionId.toString().equals(submission.path("submissionId").asText())) {
                return submission;
            }
        }
        return null;
    }

    private boolean hasQuestionSubmission(ArrayNode submissions, int questionNumber) {
        for (JsonNode submission : submissions) {
            if (submission.path("questionNo").asInt() == questionNumber) {
                return true;
            }
        }
        return false;
    }

    private boolean sameSubmission(
            JsonNode existing,
            int questionNumber,
            LearningSubmission request
    ) {
        return existing.path("questionNo").asInt() == questionNumber
                && existing.path("responseType").asText().equals(request.responseType().name())
                && existing.path("response").equals(request.response());
    }

    private Set<Integer> completedQuestionNumbers(ArrayNode submissions) {
        Set<Integer> completed = new HashSet<>();
        submissions.forEach(submission -> {
            if (submission.path("questionNo").asInt() > 0) {
                completed.add(submission.path("questionNo").asInt());
            }
        });
        return completed;
    }

    private Integer nextQuestionNumber(ArrayNode submissions, int totalQuestions) {
        Set<Integer> completed = completedQuestionNumbers(submissions);
        for (int questionNumber = 1; questionNumber <= totalQuestions; questionNumber++) {
            if (!completed.contains(questionNumber)) {
                return questionNumber;
            }
        }
        return null;
    }

    private ObjectNode writeProgress(TestProgressResponse progress) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("feedbackType", progress.feedbackType());
        node.put("submissionId", progress.submissionId().toString());
        node.put("accepted", progress.accepted());
        node.put("questionNumber", progress.questionNumber());
        node.put("completedQuestions", progress.completedQuestions());
        node.put("totalQuestions", progress.totalQuestions());
        node.put("progressPercent", progress.progressPercent());
        if (progress.nextQuestionNumber() == null) node.putNull("nextQuestionNumber");
        else node.put("nextQuestionNumber", progress.nextQuestionNumber());
        node.put("testCompleted", progress.testCompleted());
        return node;
    }

    private TestProgressResponse readProgress(JsonNode node) {
        return new TestProgressResponse(
                node.path("feedbackType").asText(),
                UUID.fromString(node.path("submissionId").asText()),
                node.path("accepted").asBoolean(),
                node.path("questionNumber").asInt(),
                node.path("completedQuestions").asInt(),
                node.path("totalQuestions").asInt(),
                node.path("progressPercent").asInt(),
                node.path("nextQuestionNumber").isNull()
                        ? null
                        : node.path("nextQuestionNumber").asInt(),
                node.path("testCompleted").asBoolean()
        );
    }
}
