package com.iread.backend.test.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.learning.app.dto.LearningSubmission;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationReferenceWord;
import com.iread.backend.pronunciation.PronunciationWordAligner;
import com.iread.backend.pronunciation.PronunciationWordResult;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.app.dto.req.*;
import com.iread.backend.test.app.dto.res.*;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.test.recommendation.TestRecommendationAfterCommitPublisher;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
import com.iread.backend.training.generation.TrainingType;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AppTestService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TestCurriculumRepository testCurriculumRepository;
    private final TestDataRepository testDataRepository;
    private final TrainingTemplateRepository trainingTemplateRepository;
    private final PersonalizedTrainingGenerationService trainingGenerationService;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    private final PronunciationWordAligner pronunciationWordAligner;
    private final AudioUploadPolicy audioUploadPolicy;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final ObjectMapper objectMapper;
    private final AppLearningQuestionSupport learningQuestionSupport;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final StudentFeatureProfileService studentFeatureProfileService;
    private final TestRecommendationAfterCommitPublisher recommendationPublisher;

    @Autowired
    public AppTestService(
            StudentRepository studentRepository,
            StudentTestRepository testRepository,
            TestCurriculumRepository testCurriculumRepository,
            TestDataRepository testDataRepository,
            TrainingTemplateRepository trainingTemplateRepository,
            PersonalizedTrainingGenerationService trainingGenerationService,
            WordRepository wordRepository,
            WordAttemptLogRepository wordAttemptLogRepository,
            PronunciationAnalysisAdapter pronunciationAnalysisAdapter,
            PronunciationWordAligner pronunciationWordAligner,
            AudioUploadPolicy audioUploadPolicy,
            WordAttemptScoreCalculator wordAttemptScoreCalculator,
            ObjectMapper objectMapper,
            AppLearningQuestionSupport learningQuestionSupport,
            RealtimeEventPublisher realtimeEventPublisher,
            StudentFeatureProfileService studentFeatureProfileService,
            TestRecommendationAfterCommitPublisher recommendationPublisher
    ) {
        this.studentRepository = studentRepository;
        this.testRepository = testRepository;
        this.testCurriculumRepository = testCurriculumRepository;
        this.testDataRepository = testDataRepository;
        this.trainingTemplateRepository = trainingTemplateRepository;
        this.trainingGenerationService = trainingGenerationService;
        this.wordRepository = wordRepository;
        this.wordAttemptLogRepository = wordAttemptLogRepository;
        this.pronunciationAnalysisAdapter = pronunciationAnalysisAdapter;
        this.pronunciationWordAligner = pronunciationWordAligner;
        this.audioUploadPolicy = audioUploadPolicy;
        this.wordAttemptScoreCalculator = wordAttemptScoreCalculator;
        this.objectMapper = objectMapper;
        this.learningQuestionSupport = learningQuestionSupport;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.studentFeatureProfileService = studentFeatureProfileService;
        this.recommendationPublisher = recommendationPublisher;
    }

    AppTestService(
            StudentRepository studentRepository,
            StudentTestRepository testRepository,
            TestDataRepository testDataRepository,
            WordRepository wordRepository,
            WordAttemptLogRepository wordAttemptLogRepository,
            PronunciationAnalysisAdapter pronunciationAnalysisAdapter,
            PronunciationWordAligner pronunciationWordAligner,
            AudioUploadPolicy audioUploadPolicy,
            WordAttemptScoreCalculator wordAttemptScoreCalculator,
            ObjectMapper objectMapper,
            AppLearningQuestionSupport learningQuestionSupport,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                pronunciationWordAligner,
                audioUploadPolicy,
                wordAttemptScoreCalculator,
                objectMapper,
                learningQuestionSupport,
                realtimeEventPublisher,
                null
        );
    }

    AppTestService(
            StudentRepository studentRepository,
            StudentTestRepository testRepository,
            TestDataRepository testDataRepository,
            WordRepository wordRepository,
            WordAttemptLogRepository wordAttemptLogRepository,
            PronunciationAnalysisAdapter pronunciationAnalysisAdapter,
            PronunciationWordAligner pronunciationWordAligner,
            AudioUploadPolicy audioUploadPolicy,
            WordAttemptScoreCalculator wordAttemptScoreCalculator,
            ObjectMapper objectMapper,
            AppLearningQuestionSupport learningQuestionSupport,
            RealtimeEventPublisher realtimeEventPublisher,
            TestRecommendationAfterCommitPublisher recommendationPublisher
    ) {
        this(
                studentRepository, testRepository, null, testDataRepository,
                null, null, wordRepository, wordAttemptLogRepository,
                pronunciationAnalysisAdapter, pronunciationWordAligner,
                audioUploadPolicy, wordAttemptScoreCalculator, objectMapper,
                learningQuestionSupport, realtimeEventPublisher,
                null,
                recommendationPublisher
        );
    }

    private static final int TRACK_QUESTION_COUNT = 3;
    private static final int TOTAL_QUESTION_COUNT = 9;
    private static final int MAX_PRONUNCIATION_ATTEMPTS = 1;
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+");
    private static final Set<TrainingType> PHONOLOGICAL_TYPES = EnumSet.range(
            TrainingType.VOWEL_TRACE,
            TrainingType.SYLLABLE_REPLACE
    );
    private static final Set<TrainingType> SHORT_TEXT_TYPES = EnumSet.range(
            TrainingType.WORD_READING,
            TrainingType.IMAGE_SENTENCE_MATCH
    );
    private static final Set<TrainingType> FLUENCY_TYPES = EnumSet.range(
            TrainingType.SENTENCE_REPEAT,
            TrainingType.SHORT_STORY_READING
    );

    @Transactional
    public SkillChallengePlanResponse getChallengePlan(Long teacherId, Long studentId) {
        StudentEntity student = studentRepository
                .findByIdAndTeacherIdForUpdate(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "학생을 찾을 수 없습니다."
                ));
        // 실력도전 플랜 선택 규칙:
        // 1) 미완료 검사가 있으면 이어서 진행 (시드된 최초 검사는 9문항으로 보충)
        // 2) 없으면 최신 완료 검사가 현행 9문항 형식일 때 완료 플랜 반환
        //    (당일 재시험 불가, 다음 검사는 새벽 배치가 생성)
        // 3) 완료된 레거시(9문항 아님) 이력뿐이거나 검사가 없으면 새 9문항 생성.
        //    완료된 검사는 문항 구성을 바꿀 수 없으므로 보충 대상에서 제외한다.
        TestCurriculumEntity curriculum = testCurriculumRepository
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(
                        studentId,
                        List.of(
                                TestStatus.NOT_STARTED.name(),
                                TestStatus.IN_PROGRESS.name()
                        )
                )
                .or(() -> latestCurrentFormatChallenge(studentId))
                .orElseGet(() -> createChallenge(student));
        List<StudentTestEntity> tests =
                testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
                        curriculum.getId()
                );
        if (tests.size() != TOTAL_QUESTION_COUNT) {
            tests = expandLegacyChallenge(student, curriculum, tests);
        }
        return toChallengePlan(curriculum, tests);
    }

    /** 최신 검사가 현행 9문항 형식일 때만 반환한다. 레거시 이력은 플랜 대상이 아니다. */
    private java.util.Optional<TestCurriculumEntity> latestCurrentFormatChallenge(Long studentId) {
        return testCurriculumRepository
                .findFirstByStudentIdOrderByCreatedAtDescIdDesc(studentId)
                .filter(curriculum -> testRepository
                        .findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(curriculum.getId())
                        .size() == TOTAL_QUESTION_COUNT);
    }

    public TestIntroResponse getIntro(Long teacherId, Long studentId, Long testId) {
        StudentTestEntity test = findOwnedTest(teacherId, studentId, testId);
        JsonNode generatedData = readGeneratedData(test.getId());
        JsonNode questions = generatedData.path("questions");
        return new TestIntroResponse(
                test.getId(),
                studentId,
                test.getTrainingTemplate().getId(),
                test.getSequenceNo(),
                test.getTrainingTemplate().getName(),
                studentGeneratedData(generatedData),
                test.getCreatedAt(),
                test.getStatus(),
                questions.size()
        );
    }

    public TestIntroResponse getIntro(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findCurrentTest(studentId, Set.of(
                TestStatus.NOT_STARTED,
                TestStatus.IN_PROGRESS
        ));
        return getIntro(teacherId, studentId, test.getId());
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
    public TestStartResponse start(Long teacherId, Long studentId, Long testId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity test = findTestForUpdate(studentId, testId);
        if (test.getStatus() == TestStatus.IN_PROGRESS) {
            return new TestStartResponse(
                    test.getId(),
                    test.getStartedAt(),
                    test.getStatus()
            );
        }
        validateStartOrder(test);
        LocalDateTime startedAt = LocalDateTime.now();
        test.start(startedAt);
        if (test.getTestCurriculum() != null) {
            test.getTestCurriculum().start();
        }
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                testId,
                "STARTED"
        );
        return new TestStartResponse(test.getId(), startedAt, test.getStatus());
    }

    @Transactional
    public TestStartResponse start(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentTestEntity current = findCurrentTest(
                studentId,
                Set.of(TestStatus.IN_PROGRESS, TestStatus.NOT_STARTED)
        );
        StudentTestEntity test = findTestForUpdate(studentId, current.getId());
        if (test.getStatus() == TestStatus.IN_PROGRESS) {
            return new TestStartResponse(
                    test.getId(),
                    test.getStartedAt(),
                    test.getStatus()
            );
        }
        validateStartOrder(test);
        LocalDateTime startedAt = LocalDateTime.now();
        test.start(startedAt);
        if (test.getTestCurriculum() != null) {
            test.getTestCurriculum().start();
        }
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                test.getId(),
                "STARTED"
        );
        return new TestStartResponse(test.getId(), startedAt, test.getStatus());
    }

    @Transactional
    public TestResetResponse reset(Long teacherId, Long studentId, Long testId) {
        StudentTestEntity test = findOwnedTestForUpdate(teacherId, studentId, testId);
        test.reset();
        wordAttemptLogRepository.deleteAllByTestId(testId);
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                testId,
                "RESET"
        );
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
        JsonNode question = findQuestion(request.testId(), questionNumber);
        Set<TrainingInputType> requiredInputs = TrainingInputPolicy.forQuestion(question);
        if (!requiredInputs.contains(TrainingInputType.VOICE)) {
            throw new IllegalArgumentException("음성 입력이 필요한 검사 문항이 아닙니다.");
        }
        boolean gazeRequired = requiredInputs.contains(TrainingInputType.GAZE);
        String expectedText = request.expectedText();
        if ((expectedText == null || expectedText.isBlank()) && request.wordId() != null) {
            expectedText = findWord(request.wordId()).getContent();
        }
        RecordingTarget target = resolveRecordingTarget(
                question,
                request.targetIndex(),
                request.tokenIndex(),
                expectedText
        );
        validateSpeechOffsets(request.speechStartOffsetMs(), request.speechEndOffsetMs());
        AudioUploadPolicy.ValidatedAudio validatedAudio =
                audioUploadPolicy.validate(request.audioFile());
        PronunciationAnalysisResult analysis = pronunciationAnalysisAdapter.analyze(
                new PronunciationAnalysisRequest(
                        "test-recording-" + request.testId() + "-" + questionNumber
                                + "-" + target.targetIndex() + "-" + System.nanoTime(),
                        target.referenceText(),
                        validatedAudio.originalFilename(),
                        validatedAudio.contentType(),
                        audioBytes(request)
                )
        );
        PronunciationWordAligner.Alignment alignment = pronunciationWordAligner.align(
                target.words(),
                analysis.words()
        );
        ObjectNode progressResult = readObjectOrNew(test.getResult());
        int attemptNo = countTargetPronunciationAnalyses(
                progressResult,
                questionNumber,
                target.targetIndex(),
                request.tokenIndex()
        ) + 1;
        if (attemptNo > MAX_PRONUNCIATION_ATTEMPTS) {
            throw new ConflictException("발음 문항의 최대 시도 횟수를 초과했습니다.");
        }
        List<StoredPronunciationWord> storedWords = storePronunciationWords(
                student,
                test,
                questionNumber,
                request,
                target,
                alignment,
                gazeRequired,
                attemptNo - 1
        );
        boolean passed = wordAttemptScoreCalculator.meetsPronunciationThreshold(
                analysis.pronunciationAccuracyScore()
        ) && storedWords.stream().allMatch(value ->
                Boolean.TRUE.equals(value.attempt().getCorrect()));
        boolean questionCompleted = passed || attemptNo == MAX_PRONUNCIATION_ATTEMPTS;
        boolean canRetry = !questionCompleted;
        recordPronunciationAnalysis(
                test,
                questionNumber,
                target,
                storedWords,
                analysis,
                alignment.insertionCount(),
                request.tokenIndex(),
                attemptNo,
                passed,
                questionCompleted
        );
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                test.getId(),
                "PROGRESS_UPDATED"
        );
        LocalDateTime createdAt = storedWords.stream()
                .map(value -> value.attempt().getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseGet(LocalDateTime::now);
        return new TestRecordingResponse(
                test.getId(),
                questionNumber,
                analysis.pronunciationAccuracyScore(),
                analysis.fluencyScore(),
                analysis.completenessScore(),
                analysis.pronScore(),
                analysis.confidence(),
                analysis.analysisVersion(),
                wordAttemptScoreCalculator.pronunciationThreshold(),
                attemptNo,
                MAX_PRONUNCIATION_ATTEMPTS,
                passed,
                questionCompleted,
                canRetry,
                storedWords.stream().map(this::toRecordingWordResult).toList(),
                createdAt
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
            return progressForExistingSubmission(existing, submissions, questions.size());
        }
        JsonNode existingQuestionSubmission =
                findQuestionSubmission(submissions, questionNumber);
        if (existingQuestionSubmission != null) {
            return progressForExistingSubmission(
                    existingQuestionSubmission,
                    submissions,
                    questions.size()
            );
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
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                test.getId(),
                "PROGRESS_UPDATED"
        );
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
        Set<Integer> completedQuestionNumbers = completedQuestionNumbers(submissions);
        legacyAttempts.stream()
                .map(WordAttemptLogEntity::getQuestionNo)
                .filter(java.util.Objects::nonNull)
                .forEach(completedQuestionNumbers::add);
        int completedQuestions = completedQuestionNumbers.size();
        if (completedQuestions != questions.size()) {
            throw new ConflictException("모든 검사 문항을 제출한 후 검사를 종료할 수 있습니다.");
        }
        if (legacyAttempts.stream().anyMatch(
                attempt -> attempt.getTotalScore() == null
        )) {
            throw new ConflictException(
                    "단어별 필수 입력 점수가 모두 계산된 후 검사를 종료할 수 있습니다."
            );
        }
        Map<Integer, Integer> questionScores = new HashMap<>();
        for (JsonNode submission : submissions) {
            questionScores.put(
                    submission.path("questionNo").asInt(),
                    submission.path("totalScore").asInt()
            );
        }
        Map<Integer, List<WordAttemptLogEntity>> attemptsByQuestion =
                legacyAttempts.stream().collect(java.util.stream.Collectors.groupingBy(
                        WordAttemptLogEntity::getQuestionNo
                ));
        for (Map.Entry<Integer, List<WordAttemptLogEntity>> entry
                : attemptsByQuestion.entrySet()) {
            questionScores.put(
                    entry.getKey(),
                    (int) Math.round(entry.getValue().stream()
                            .map(WordAttemptLogEntity::getTotalScore)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0))
            );
        }
        long scoreSum = questionScores.values().stream()
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal accuracy = BigDecimal.valueOf(scoreSum)
                .divide(BigDecimal.valueOf(completedQuestions * 10L), 2, RoundingMode.HALF_UP);
        LocalDateTime completedAt = LocalDateTime.now();
        if (test.getStartedAt() != null) {
            long solvingTimeSeconds = Math.max(
                    0,
                    Duration.between(test.getStartedAt(), completedAt).getSeconds()
            );
            result.put("solvingTimeSeconds", solvingTimeSeconds);
        }
        test.complete(writeJson(result), accuracy, completedAt);
        Long completedCurriculumId = null;
        if (test.getTestCurriculum() != null) {
            List<StudentTestEntity> curriculumTests =
                    testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
                            test.getTestCurriculum().getId()
                    );
            if (curriculumTests.stream().allMatch(
                    item -> item.getStatus() == TestStatus.COMPLETED
            )) {
                boolean completedNow = test.getTestCurriculum().complete(completedAt);
                if (completedNow) {
                    completedCurriculumId = test.getTestCurriculum().getId();
                }
            }
        }
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        studentFeatureProfileService.recalculate(student);
        if (completedCurriculumId != null && recommendationPublisher != null) {
            recommendationPublisher.processAfterCommit(completedCurriculumId);
        }
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TEST,
                test.getId(),
                "COMPLETED"
        );
        return completionResponse(test);
    }

    private TestCurriculumEntity createChallenge(StudentEntity student) {
        return createChallenge(student, LocalDateTime.now());
    }

    /**
     * 9문항 실력도전 검사(분류별 3문항)와 문항 데이터를 생성한다.
     * 데모 시드가 과거 시점의 검사 이력을 만들 때 재사용할 수 있도록 생성 시각을 받는다.
     */
    @Transactional
    public TestCurriculumEntity createChallenge(StudentEntity student, LocalDateTime createdAt) {
        List<TrainingTemplateEntity> templates = trainingTemplateRepository
                .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc()
                .stream()
                .filter(TrainingCatalogPolicy::isSelectable)
                .toList();
        List<TrainingTemplateEntity> selected = new ArrayList<>(TOTAL_QUESTION_COUNT);
        selected.addAll(selectTemplates(templates, PHONOLOGICAL_TYPES, "음운 인식 및 파닉스"));
        selected.addAll(selectTemplates(templates, SHORT_TEXT_TYPES, "글 해독 및 문장 이해"));
        selected.addAll(selectTemplates(templates, FLUENCY_TYPES, "유창성"));

        TestCurriculumEntity curriculum = testCurriculumRepository.saveAndFlush(
                new TestCurriculumEntity(nextCurriculumId(), student, createdAt)
        );
        for (int index = 0; index < selected.size(); index++) {
            TrainingTemplateEntity template = selected.get(index);
            StudentTestEntity test = testRepository.saveAndFlush(
                    new StudentTestEntity(curriculum, template, index + 1)
            );
            ObjectNode generated = trainingGenerationService.generateTestQuestion(
                    student.getId(),
                    template,
                    "skill-challenge-" + curriculum.getId() + "-" + (index + 1)
            );
            testDataRepository.save(
                    new TestDataEntity(
                            nextTestDataId(),
                            test,
                            writeJson(generated),
                            createdAt
                    )
            );
        }
        return curriculum;
    }

    private List<StudentTestEntity> expandLegacyChallenge(
            StudentEntity student,
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> existing
    ) {
        if (existing.stream().anyMatch(test -> test.getStatus() != TestStatus.NOT_STARTED)) {
            throw new ConflictException(
                    "진행 중인 기존 검사는 9문항 실력도전으로 자동 전환할 수 없습니다."
            );
        }
        List<TrainingTemplateEntity> allTemplates = trainingTemplateRepository
                .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc()
                .stream()
                .filter(TrainingCatalogPolicy::isSelectable)
                .toList();
        LocalDateTime createdAt = LocalDateTime.now();
        List<StudentTestEntity> result = new ArrayList<>(existing);
        for (int trackIndex = 0; trackIndex < 3; trackIndex++) {
            int startSequence = trackIndex * TRACK_QUESTION_COUNT + 1;
            int endSequence = startSequence + TRACK_QUESTION_COUNT - 1;
            Set<TrainingType> types = switch (trackIndex) {
                case 0 -> PHONOLOGICAL_TYPES;
                case 1 -> SHORT_TEXT_TYPES;
                default -> FLUENCY_TYPES;
            };
            List<StudentTestEntity> trackTests = result.stream()
                    .filter(test -> test.getSequenceNo() >= startSequence
                            && test.getSequenceNo() <= endSequence)
                    .toList();
            if (trackTests.stream().anyMatch(
                    test -> !types.contains(templateType(test.getTrainingTemplate()))
            )) {
                throw new ConflictException(
                        "기존 검사 템플릿 분류가 실력도전 순서와 일치하지 않습니다."
                );
            }
            Set<Long> usedTemplateIds = trackTests.stream()
                    .map(test -> test.getTrainingTemplate().getId())
                    .collect(java.util.stream.Collectors.toSet());
            List<TrainingTemplateEntity> candidates = allTemplates.stream()
                    .filter(template -> types.contains(templateType(template)))
                    .filter(template -> !usedTemplateIds.contains(template.getId()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            Collections.shuffle(candidates);
            int candidateIndex = 0;
            for (int sequence = startSequence; sequence <= endSequence; sequence++) {
                final int currentSequence = sequence;
                StudentTestEntity test = trackTests.stream()
                        .filter(item -> item.getSequenceNo() == currentSequence)
                        .findFirst()
                        .orElse(null);
                if (test == null) {
                    if (candidateIndex >= candidates.size()) {
                        throw new ConflictException(
                                "실력도전 분류별 템플릿이 3개 이상 필요합니다."
                        );
                    }
                    test = testRepository.saveAndFlush(
                            new StudentTestEntity(
                                    curriculum,
                                    candidates.get(candidateIndex++),
                                    sequence
                            )
                    );
                    result.add(test);
                }
                ObjectNode generated = trainingGenerationService.generateTestQuestion(
                        student.getId(),
                        test.getTrainingTemplate(),
                        "skill-challenge-" + curriculum.getId() + "-" + sequence
                );
                testDataRepository.save(
                        new TestDataEntity(
                                nextTestDataId(),
                                test,
                                writeJson(generated),
                                createdAt
                        )
                );
            }
        }
        return result.stream()
                .sorted(java.util.Comparator.comparing(StudentTestEntity::getSequenceNo))
                .toList();
    }

    private List<TrainingTemplateEntity> selectTemplates(
            Collection<TrainingTemplateEntity> templates,
            Set<TrainingType> acceptedTypes,
            String trackTitle
    ) {
        List<TrainingTemplateEntity> candidates = templates.stream()
                .filter(template -> acceptedTypes.contains(templateType(template)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (candidates.size() < TRACK_QUESTION_COUNT) {
            throw new ConflictException(
                    trackTitle + " 실력도전용 훈련 템플릿이 3개 이상 필요합니다."
            );
        }
        Collections.shuffle(candidates);
        return List.copyOf(candidates.subList(0, TRACK_QUESTION_COUNT));
    }

    private TrainingType templateType(TrainingTemplateEntity template) {
        try {
            return TrainingType.from(
                    objectMapper.readTree(template.getPrompt())
                            .path("trainingType")
                            .asText()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "훈련 템플릿의 trainingType을 읽을 수 없습니다: " + template.getId(),
                    exception
            );
        }
    }

    private SkillChallengePlanResponse toChallengePlan(
            TestCurriculumEntity curriculum,
            List<StudentTestEntity> tests
    ) {
        List<SkillChallengePlanResponse.Track> tracks = List.of(
                toTrack("phonological", "음운 인식", tests.subList(0, 3)),
                toTrack("short-text", "짧은 글", tests.subList(3, 6)),
                toTrack("fluency", "유창성", tests.subList(6, 9))
        );
        int completed = (int) tests.stream()
                .filter(test -> test.getStatus() == TestStatus.COMPLETED)
                .count();
        StudentTestEntity next = tests.stream()
                .filter(test -> test.getStatus() == TestStatus.IN_PROGRESS)
                .findFirst()
                .orElseGet(() -> tests.stream()
                        .filter(test -> test.getStatus() == TestStatus.NOT_STARTED)
                        .findFirst()
                        .orElse(null));
        return new SkillChallengePlanResponse(
                curriculum.getId(),
                completed,
                TOTAL_QUESTION_COUNT,
                completed == TOTAL_QUESTION_COUNT,
                next == null ? null : next.getId(),
                next == null ? null : trackCode(next.getSequenceNo()),
                tracks
        );
    }

    private String trackCode(int sequenceNo) {
        if (sequenceNo <= TRACK_QUESTION_COUNT) {
            return "phonological";
        }
        if (sequenceNo <= TRACK_QUESTION_COUNT * 2) {
            return "short-text";
        }
        if (sequenceNo <= TOTAL_QUESTION_COUNT) {
            return "fluency";
        }
        throw new IllegalStateException("실력도전 검사 순서가 올바르지 않습니다: " + sequenceNo);
    }

    private SkillChallengePlanResponse.Track toTrack(
            String code,
            String title,
            List<StudentTestEntity> tests
    ) {
        int completed = (int) tests.stream()
                .filter(test -> test.getStatus() == TestStatus.COMPLETED)
                .count();
        StudentTestEntity next = tests.stream()
                .filter(test -> test.getStatus() == TestStatus.IN_PROGRESS)
                .findFirst()
                .orElseGet(() -> tests.stream()
                        .filter(test -> test.getStatus() == TestStatus.NOT_STARTED)
                        .findFirst()
                        .orElse(null));
        String status = completed == TRACK_QUESTION_COUNT
                ? TestStatus.COMPLETED.name()
                : completed > 0 || tests.stream().anyMatch(
                        test -> test.getStatus() == TestStatus.IN_PROGRESS
                )
                ? TestStatus.IN_PROGRESS.name()
                : TestStatus.NOT_STARTED.name();
        return new SkillChallengePlanResponse.Track(
                code,
                title,
                status,
                completed,
                TRACK_QUESTION_COUNT,
                next == null ? null : next.getId()
        );
    }

    private void validateStartOrder(StudentTestEntity test) {
        if (test.getStatus() != TestStatus.NOT_STARTED) {
            throw new ConflictException("시작 가능한 검사가 아닙니다.");
        }
        if (test.getTestCurriculum() == null) {
            return;
        }
        List<StudentTestEntity> tests =
                testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
                        test.getTestCurriculum().getId()
                );
        boolean previousIncomplete = tests.stream()
                .anyMatch(item -> item.getSequenceNo() < test.getSequenceNo()
                        && item.getStatus() != TestStatus.COMPLETED);
        if (previousIncomplete) {
            throw new ConflictException("앞 순번의 실력도전 문항을 먼저 완료해야 합니다.");
        }
        boolean anotherInProgress = tests.stream()
                .anyMatch(item -> !item.getId().equals(test.getId())
                        && item.getStatus() == TestStatus.IN_PROGRESS);
        if (anotherInProgress) {
            throw new ConflictException("진행 중인 실력도전 문항을 먼저 완료해야 합니다.");
        }
    }

    private long nextCurriculumId() {
        long id;
        do {
            id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        } while (testCurriculumRepository.existsById(id));
        return id;
    }

    private long nextTestDataId() {
        long id;
        do {
            id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        } while (testDataRepository.existsById(id));
        return id;
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
        if (statuses.contains(TestStatus.IN_PROGRESS)) {
            var inProgress = testRepository
                    .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                            studentId,
                            Set.of(TestStatus.IN_PROGRESS)
                    );
            if (inProgress.isPresent()) {
                return inProgress.get();
            }
        }
        if (statuses.contains(TestStatus.NOT_STARTED)) {
            var notStarted = testRepository
                    .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                            studentId,
                            Set.of(TestStatus.NOT_STARTED)
                    );
            if (notStarted.isPresent()) {
                return notStarted.get();
            }
        }
        throw new ResourceNotFoundException("진행할 검사를 찾을 수 없습니다.");
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

    private JsonNode findQuestion(Long testId, int questionNumber) {
        JsonNode questions = readQuestions(testId);
        if (questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("검사 문항을 찾을 수 없습니다.");
        }
        return questions.get(questionNumber - 1);
    }

    private JsonNode readQuestions(Long testId) {
        JsonNode generatedData = readGeneratedData(testId);
        JsonNode questions = generatedData.path("questions");
        if (!questions.isArray()) {
            throw new IllegalStateException("저장된 검사 문항 형식이 올바르지 않습니다.");
        }
        ArrayNode normalized = objectMapper.createArrayNode();
        questions.forEach(question -> {
            if (!question.isObject()) {
                throw new IllegalStateException("저장된 검사 문항 형식이 올바르지 않습니다.");
            }
            ObjectNode copy = (ObjectNode) question.deepCopy();
            TrainingType type = TrainingType.from(copy.path("type").asText());
            ArrayNode requiredInputs = copy.putArray("requiredInputs");
            TrainingInputPolicy.expectedFor(type).forEach(input ->
                    requiredInputs.add(input.name())
            );
            normalized.add(copy);
        });
        return normalized;
    }

    private JsonNode readGeneratedData(Long testId) {
        return testDataRepository
                .findFirstByTestIdOrderByCreatedAtDescIdDesc(testId)
                .map(TestDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("검사 문항을 찾을 수 없습니다."));
    }

    private ObjectNode studentGeneratedData(JsonNode generatedData) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("schemaVersion", generatedData.path("schemaVersion").asInt(2));
        ArrayNode questions = result.putArray("questions");
        generatedData.path("questions").forEach(question ->
                questions.add(learningQuestionSupport.toStudentQuestion(question)));
        return result;
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

    private void recordPronunciationAnalysis(
            StudentTestEntity test,
            int questionNumber,
            RecordingTarget target,
            List<StoredPronunciationWord> storedWords,
            PronunciationAnalysisResult analysis,
            int insertionCount,
            Integer evaluatedTokenIndex,
            int attemptNo,
            boolean passed,
            boolean questionCompleted
    ) {
        ObjectNode result = readObjectOrNew(test.getResult());
        result.put("schemaVersion", 2);
        ArrayNode wordAttempts = result.withArray("wordAttempts");
        for (StoredPronunciationWord stored : storedWords) {
            Integer tokenIndex = stored.reference().tokenIndex();
            wordAttempts.forEach(existing -> {
                if (existing.path("questionNo").asInt() == questionNumber
                        && existing.path("targetIndex").asInt() == target.targetIndex()
                        && java.util.Objects.equals(
                        nullableInt(existing, "tokenIndex"),
                        tokenIndex
                )) {
                    ((ObjectNode) existing).put("isFinal", false);
                }
            });
            ObjectNode attemptLink = wordAttempts.addObject();
            attemptLink.put("wordAttemptLogId", stored.attempt().getId());
            attemptLink.put("questionNo", questionNumber);
            attemptLink.put("targetIndex", target.targetIndex());
            if (tokenIndex == null) attemptLink.putNull("tokenIndex");
            else attemptLink.put("tokenIndex", tokenIndex);
            attemptLink.put("isFinal", true);
            attemptLink.put("referenceText", stored.reference().surface());
            attemptLink.put(
                    "pronunciationAccuracyScore",
                    stored.analyzed().scoreOrZero()
            );
            attemptLink.put(
                    "pronunciationErrorType",
                    stored.analyzed().errorType()
            );
            attemptLink.put(
                    "pronunciationAnalysisVersion",
                    analysis.analysisVersion()
            );
            attemptLink.put("wordReadTimeMs", stored.analyzed().durationMs());
        }

        ObjectNode analysisLink = result.withArray("pronunciationAnalyses").addObject();
        analysisLink.put("questionNo", questionNumber);
        analysisLink.put("targetIndex", target.targetIndex());
        if (evaluatedTokenIndex == null) analysisLink.putNull("tokenIndex");
        else analysisLink.put("tokenIndex", evaluatedTokenIndex);
        analysisLink.put("referenceText", target.referenceText());
        analysisLink.put(
                "pronunciationAccuracyScore",
                analysis.pronunciationAccuracyScore()
        );
        putNullableScore(analysisLink, "fluencyScore", analysis.fluencyScore());
        putNullableScore(
                analysisLink,
                "completenessScore",
                analysis.completenessScore()
        );
        putNullableScore(analysisLink, "pronScore", analysis.pronScore());
        analysisLink.put("confidence", analysis.confidence());
        analysisLink.put("analysisVersion", analysis.analysisVersion());
        analysisLink.put("insertionCount", insertionCount);
        analysisLink.put("attemptNo", attemptNo);
        analysisLink.put("passed", passed);
        analysisLink.put("questionCompleted", questionCompleted);
        test.updateResult(writeJson(result));
    }

    private RecordingTarget resolveRecordingTarget(
            JsonNode question,
            Integer targetIndex,
            Integer tokenIndex,
            String expectedText
    ) {
        int resolvedTargetIndex = resolveTargetIndex(
                question,
                targetIndex,
                tokenIndex,
                expectedText
        );
        JsonNode expected = tokenIndex == null
                ? question.path("analysisTargets").path(resolvedTargetIndex)
                : findQuestionWord(question, tokenIndex);
        if (!expected.isObject()
                && tokenIndex == null
                && expectedText != null
                && !expectedText.isBlank()) {
            return new RecordingTarget(
                    resolvedTargetIndex,
                    expectedText,
                    List.of(new PronunciationReferenceWord(null, expectedText))
            );
        }
        if (!expected.isObject()) {
            throw new IllegalArgumentException("요청한 분석 대상 위치가 올바르지 않습니다.");
        }
        String storedText = tokenIndex == null
                ? expected.path("text").asText()
                : expected.path("surface").asText();
        if (!expectedText.equals(storedText)) {
            throw new IllegalArgumentException(
                    "요청한 텍스트가 생성된 검사 문항과 일치하지 않습니다."
            );
        }
        if (tokenIndex != null) {
            return new RecordingTarget(
                    resolvedTargetIndex,
                    storedText,
                    List.of(new PronunciationReferenceWord(tokenIndex, storedText))
            );
        }
        if (storedText.equals(question.path("text").asText())
                && question.path("words").isArray()
                && !question.path("words").isEmpty()) {
            List<PronunciationReferenceWord> words = new ArrayList<>();
            question.path("words").forEach(word -> words.add(
                    new PronunciationReferenceWord(
                            word.path("wordIndex").asInt(),
                            word.path("surface").asText()
                    )
            ));
            return new RecordingTarget(resolvedTargetIndex, storedText, words);
        }
        List<PronunciationReferenceWord> tokenized = tokenize(storedText);
        if (tokenized.size() == 1) {
            tokenized = List.of(new PronunciationReferenceWord(
                    null,
                    tokenized.getFirst().surface()
            ));
        }
        return new RecordingTarget(resolvedTargetIndex, storedText, tokenized);
    }

    private int resolveTargetIndex(
            JsonNode question,
            Integer targetIndex,
            Integer tokenIndex,
            String expectedText
    ) {
        if (targetIndex != null) {
            if (!question.path("analysisTargets").path(targetIndex).isObject()) {
                throw new IllegalArgumentException(
                        "요청한 targetIndex에 해당하는 분석 대상을 찾을 수 없습니다."
                );
            }
            return targetIndex;
        }
        if (tokenIndex != null) {
            throw new IllegalArgumentException(
                    "단일 단어 녹음에는 targetIndex가 필요합니다."
            );
        }
        int matchedIndex = -1;
        JsonNode targets = question.path("analysisTargets");
        if (!targets.isArray() || targets.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < targets.size(); index++) {
            if (expectedText.equals(targets.path(index).path("text").asText())) {
                if (matchedIndex >= 0) {
                    throw new IllegalArgumentException(
                            "같은 텍스트의 분석 대상이 여러 개이므로 targetIndex가 필요합니다."
                    );
                }
                matchedIndex = index;
            }
        }
        if (matchedIndex < 0) {
            throw new IllegalArgumentException(
                    "요청한 텍스트와 일치하는 분석 대상을 찾을 수 없습니다."
            );
        }
        return matchedIndex;
    }

    private JsonNode findQuestionWord(JsonNode question, int tokenIndex) {
        for (JsonNode word : question.path("words")) {
            if (word.path("wordIndex").asInt(-1) == tokenIndex) {
                return word;
            }
        }
        return objectMapper.missingNode();
    }

    private List<PronunciationReferenceWord> tokenize(String text) {
        List<PronunciationReferenceWord> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            words.add(new PronunciationReferenceWord(words.size(), matcher.group()));
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("발음 평가할 단어를 찾을 수 없습니다.");
        }
        return List.copyOf(words);
    }

    private List<StoredPronunciationWord> storePronunciationWords(
            StudentEntity student,
            StudentTestEntity test,
            int questionNumber,
            TestRecordingRequest request,
            RecordingTarget target,
            PronunciationWordAligner.Alignment alignment,
            boolean gazeRequired,
            int retryCount
    ) {
        List<StoredPronunciationWord> values = new ArrayList<>();
        for (PronunciationWordAligner.AlignedWord aligned : alignment.words()) {
            PronunciationReferenceWord reference = aligned.reference();
            PronunciationWordResult analyzed = aligned.analyzed();
            WordEntity word = resolveWord(
                    reference.surface(),
                    target.words().size() == 1 ? request.wordId() : null
            );
            int pronunciationAccuracyScore =
                    (int) Math.round(analyzed.scoreOrZero() * 10);
            boolean correct = wordAttemptScoreCalculator
                    .meetsPronunciationThreshold(pronunciationAccuracyScore)
                    && "NONE".equalsIgnoreCase(analyzed.errorType());
            Integer totalScore = wordAttemptScoreCalculator.calculate(
                    pronunciationAccuracyScore,
                    true,
                    true,
                    analyzed.isOmission(),
                    gazeRequired,
                    false,
                    null,
                    null,
                    retryCount,
                    correct
            );
            markPreviousAttemptsNotFinal(
                    test.getId(),
                    questionNumber,
                    target.targetIndex(),
                    reference.tokenIndex()
            );
            int speechStartOffsetMs = analyzed.offsetMs();
            int speechEndOffsetMs = analyzed.offsetMs() + analyzed.durationMs();
            WordAttemptLogEntity attempt = WordAttemptLogEntity.forTest(
                    student,
                    word,
                    test,
                    reference.surface(),
                    true,
                    pronunciationAccuracyScore,
                    speechStartOffsetMs,
                    speechEndOffsetMs,
                    analyzed.isOmission(),
                    correct,
                    totalScore,
                    questionNumber,
                    target.targetIndex(),
                    reference.tokenIndex()
            );
            values.add(new StoredPronunciationWord(reference, analyzed, attempt));
        }
        List<WordAttemptLogEntity> saved;
        if (values.size() == 1) {
            saved = List.of(wordAttemptLogRepository.saveAndFlush(
                    values.getFirst().attempt()
            ));
        } else {
            saved = wordAttemptLogRepository.saveAllAndFlush(
                    values.stream().map(StoredPronunciationWord::attempt).toList()
            );
        }
        for (int index = 0; index < values.size(); index++) {
            values.set(index, new StoredPronunciationWord(
                    values.get(index).reference(),
                    values.get(index).analyzed(),
                    saved.get(index)
            ));
        }
        return List.copyOf(values);
    }

    private WordEntity resolveWord(String surface, Long requestedWordId) {
        if (requestedWordId != null) {
            WordEntity requested = findWord(requestedWordId);
            if (!surface.equals(requested.getContent())) {
                throw new IllegalArgumentException(
                        "wordId와 발음 평가 대상 단어가 일치하지 않습니다."
                );
            }
            return requested;
        }
        return wordRepository.findByContent(surface)
                .orElseGet(() -> wordRepository.save(new WordEntity(surface)));
    }

    private void markPreviousAttemptsNotFinal(
            Long testId,
            int questionNumber,
            int targetIndex,
            Integer tokenIndex
    ) {
        wordAttemptLogRepository
                .findAllByTestIdAndQuestionNoAndFinalAttemptTrue(
                        testId,
                        questionNumber
                )
                .stream()
                .filter(attempt -> java.util.Objects.equals(
                        attempt.getTargetIndex(),
                        targetIndex
                ))
                .filter(attempt -> java.util.Objects.equals(
                        attempt.getTokenIndex(),
                        tokenIndex
                ))
                .forEach(WordAttemptLogEntity::markNotFinal);
    }

    private int countTargetPronunciationAnalyses(
            ObjectNode result,
            int questionNumber,
            int targetIndex,
            Integer tokenIndex
    ) {
        int count = 0;
        for (JsonNode analysis : result.withArray("pronunciationAnalyses")) {
            if (analysis.path("questionNo").asInt() == questionNumber
                    && analysis.path("targetIndex").asInt() == targetIndex
                    && java.util.Objects.equals(
                    nullableInt(analysis, "tokenIndex"),
                    tokenIndex
            )) {
                count++;
            }
        }
        return count;
    }

    private TestRecordingResponse.WordResult toRecordingWordResult(
            StoredPronunciationWord stored
    ) {
        return new TestRecordingResponse.WordResult(
                stored.attempt().getId(),
                stored.attempt().getWord().getId(),
                stored.reference().tokenIndex(),
                stored.reference().surface(),
                stored.analyzed().scoreOrZero(),
                stored.analyzed().errorType(),
                stored.attempt().getTotalScore(),
                stored.attempt().getSpeechStartOffsetMs(),
                stored.attempt().getSpeechEndOffsetMs(),
                stored.attempt().getCreatedAt()
        );
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asInt() : null;
    }

    private record RecordingTarget(
            int targetIndex,
            String referenceText,
            List<PronunciationReferenceWord> words
    ) {
        private RecordingTarget {
            words = List.copyOf(words);
        }
    }

    private record StoredPronunciationWord(
            PronunciationReferenceWord reference,
            PronunciationWordResult analyzed,
            WordAttemptLogEntity attempt
    ) {
    }

    private void putNullableScore(ObjectNode node, String field, Double value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
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

    private JsonNode findQuestionSubmission(ArrayNode submissions, int questionNumber) {
        for (JsonNode submission : submissions) {
            if (submission.path("questionNo").asInt() == questionNumber) {
                return submission;
            }
        }
        return null;
    }

    private TestProgressResponse progressForExistingSubmission(
            JsonNode existing,
            ArrayNode submissions,
            int totalQuestions
    ) {
        JsonNode savedProgress = existing.path("progress");
        if (savedProgress.isObject() && savedProgress.hasNonNull("submissionId")) {
            return readProgress(savedProgress);
        }

        int questionNumber = existing.path("questionNo").asInt();
        int completedQuestions = completedQuestionNumbers(submissions).size();
        return new TestProgressResponse(
                "TEST_PROGRESS",
                UUID.fromString(existing.path("submissionId").asText()),
                true,
                questionNumber,
                completedQuestions,
                totalQuestions,
                completedQuestions * 100 / totalQuestions,
                nextQuestionNumber(submissions, totalQuestions),
                completedQuestions == totalQuestions
        );
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
