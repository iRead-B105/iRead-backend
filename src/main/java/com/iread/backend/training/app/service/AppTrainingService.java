package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.learning.app.dto.LearningErrorLocation;
import com.iread.backend.learning.app.dto.LearningSubmission;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.pronunciation.*;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.service.TrainingService;
import com.iread.backend.training.app.dto.req.TrainingRecordingRequest;
import com.iread.backend.training.app.dto.res.*;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTrainingService {
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+");
    private static final int MAX_SELECTION_ATTEMPTS = 2;
    private static final int MAX_PRONUNCIATION_ATTEMPTS = 2;

    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    private final AudioUploadPolicy audioUploadPolicy;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final PronunciationWordAligner pronunciationWordAligner;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final TrainingService trainingService;
    private final ObjectMapper objectMapper;
    private final AppLearningQuestionSupport learningQuestionSupport;
    private final RealtimeEventPublisher realtimeEventPublisher;

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
                learningQuestionSupport.toStudentQuestion(questions.get(questionNumber - 1))
        );
    }

    @Transactional
    public TrainingStartResponse start(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTrainingForUpdate(teacherId, studentId, trainingId);
        if (training.getStatus() != TrainingStatus.NOT_STARTED) {
            throw new ConflictException("시작 가능한 훈련이 아닙니다.");
        }
        LocalDateTime startedAt = LocalDateTime.now();
        training.start(startedAt);
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "STARTED"
        );
        return new TrainingStartResponse(trainingId, startedAt, training.getStatus());
    }

    @Transactional
    public TrainingResetResponse reset(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTrainingForUpdate(teacherId, studentId, trainingId);
        training.reset();
        wordAttemptLogRepository.deleteAllByTrainingId(trainingId);
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "RESET"
        );
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
        TrainingEntity training = findInProgressTrainingForUpdate(studentId, trainingId);
        trainingInputRequirementService.requireQuestionInput(
                trainingId,
                questionNumber,
                TrainingInputType.VOICE
        );
        boolean gazeRequired = trainingInputRequirementService
                .inputsForQuestion(trainingId, questionNumber)
                .contains(TrainingInputType.GAZE);
        RecordingTarget target = resolveRecordingTarget(
                trainingId,
                questionNumber,
                request.targetIndex(),
                request.tokenIndex(),
                request.expectedText()
        );
        validateSpeechOffsets(request.speechStartOffsetMs(), request.speechEndOffsetMs());
        audioUploadPolicy.validate(request.audioFile());
        PronunciationAnalysisResult analysis = pronunciationAnalysisAdapter.analyze(
                new PronunciationAnalysisRequest(
                        "training-recording-" + trainingId + "-" + questionNumber
                                + "-" + target.targetIndex() + "-" + System.nanoTime(),
                        target.referenceText(),
                        request.audioFile().getOriginalFilename(),
                        request.audioFile().getContentType(),
                        audioBytes(request)
                )
        );
        PronunciationWordAligner.Alignment alignment = pronunciationWordAligner.align(
                target.words(),
                analysis.words()
        );
        ObjectNode progressResult = readObjectOrNew(training.getResult());
        int attemptNo = countQuestionPronunciationAnalyses(
                progressResult,
                questionNumber
        ) + 1;
        if (attemptNo > MAX_PRONUNCIATION_ATTEMPTS) {
            throw new ConflictException("발음 문항의 최대 시도 횟수를 초과했습니다.");
        }
        List<StoredPronunciationWord> storedWords = storePronunciationWords(
                student,
                training,
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
        recordAttemptLinks(
                training,
                questionNumber,
                request,
                target,
                storedWords,
                analysis,
                alignment.insertionCount(),
                attemptNo,
                passed,
                questionCompleted
        );
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "PROGRESS_UPDATED"
        );
        LocalDateTime createdAt = storedWords.stream()
                .map(value -> value.attempt().getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseGet(LocalDateTime::now);
        return new TrainingRecordingResponse(
                trainingId,
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
    public TrainingFeedbackResponse saveSelection(
            Long teacherId,
            Long studentId,
            Long trainingId,
            int questionNumber,
            LearningSubmission request
    ) {
        findOwnedStudent(teacherId, studentId);
        TrainingEntity training = findInProgressTrainingForUpdate(studentId, trainingId);
        JsonNode question = findQuestion(trainingId, questionNumber);
        ObjectNode result = readObjectOrNew(training.getResult());
        result.put("schemaVersion", 2);
        ArrayNode submissions = result.withArray("submissions");

        JsonNode existing = findSubmission(submissions, request.submissionId());
        if (existing != null) {
            if (!sameSubmission(existing, questionNumber, request)) {
                throw new ConflictException("같은 submissionId를 다른 제출에 재사용할 수 없습니다.");
            }
            return readFeedback(existing.path("feedback"));
        }
        if (isQuestionCompleted(result, questionNumber)) {
            throw new ConflictException("이미 완료된 훈련 문항입니다.");
        }

        int attemptNo = countQuestionSubmissions(submissions, questionNumber) + 1;
        boolean correctionRequired = attemptNo > MAX_SELECTION_ATTEMPTS;
        AppLearningQuestionSupport.Evaluation evaluation =
                learningQuestionSupport.evaluate(question, request);
        boolean questionCompleted = evaluation.correct();
        boolean canRetry = !questionCompleted;
        String hint = evaluation.correct() ? null : hint(attemptNo);
        JsonNode correctResponse =
                !evaluation.correct() && attemptNo >= MAX_SELECTION_ATTEMPTS
                ? evaluation.correctResponse()
                : null;
        TrainingFeedbackResponse feedback = new TrainingFeedbackResponse(
                "TRAINING_FEEDBACK",
                request.submissionId(),
                attemptNo,
                MAX_SELECTION_ATTEMPTS,
                Math.max(MAX_SELECTION_ATTEMPTS - attemptNo, 0),
                evaluation.correct(),
                questionCompleted,
                canRetry,
                evaluation.errorLocations(),
                hint,
                correctResponse
        );

        ObjectNode submission = submissions.addObject();
        submission.put("submissionId", request.submissionId().toString());
        submission.put("questionNo", questionNumber);
        submission.put("responseType", request.responseType().name());
        submission.set("response", request.response().deepCopy());
        submission.put("attemptNo", attemptNo);
        submission.put("correct", evaluation.correct());
        submission.put("totalScore", evaluation.totalScore());
        submission.put("questionCompleted", questionCompleted);
        submission.put("submittedAt", LocalDateTime.now().toString());
        submission.set("feedback", writeFeedback(feedback));

        if (questionCompleted) {
            boolean scoredCorrect = !correctionRequired;
            upsertQuestionResult(
                    result,
                    questionNumber,
                    scoredCorrect,
                    scoredCorrect ? evaluation.totalScore() : 0,
                    request.submissionId(),
                    correctionRequired
            );
        }
        training.recordProgressResult(writeJson(result));
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "PROGRESS_UPDATED"
        );
        return feedback;
    }

    @Transactional
    public TrainingCompleteResponse complete(
            Long teacherId,
            Long studentId,
            Long trainingId
    ) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        if (training.getStatus() == TrainingStatus.COMPLETED) {
            return completionResponse(training);
        }
        ObjectNode result = readObjectOrNew(training.getResult());
        int totalQuestions = questionCount(trainingId);
        if (completedQuestionNumbers(result).size() != totalQuestions) {
            throw new ConflictException("모든 훈련 문항을 완료한 후 훈련을 종료할 수 있습니다.");
        }
        trainingService.completeTraining(
                teacherId,
                studentId,
                trainingId,
                result,
                LocalDateTime.now()
        );
        training = findOwnedTraining(teacherId, studentId, trainingId);
        return completionResponse(training);
    }

    private TrainingCompleteResponse completionResponse(TrainingEntity training) {
        return new TrainingCompleteResponse(
                "TRAINING_COMPLETED",
                training.getId(),
                training.getStatus(),
                training.getFinishedAt(),
                "TRAINING_COMPLETE_GREAT_JOB",
                "RETURN_TO_CURRICULUM"
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

    private TrainingEntity findOwnedTrainingForUpdate(Long teacherId, Long studentId, Long trainingId) {
        findOwnedStudent(teacherId, studentId);
        TrainingEntity current = trainingRepository
                .findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
        dailyCurriculumRepository.findForUpdate(
                        current.getDailyCurriculum().getId(),
                        studentId
                )
                .orElseThrow(() -> new ResourceNotFoundException("교육과정을 찾을 수 없습니다."));
        return trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
    }

    private TrainingEntity findInProgressTrainingForUpdate(Long studentId, Long trainingId) {
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
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

    private RecordingTarget resolveRecordingTarget(
            Long trainingId,
            int questionNumber,
            Integer targetIndex,
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
        int resolvedTargetIndex = resolveTargetIndex(
                question,
                targetIndex,
                tokenIndex,
                expectedText
        );
        JsonNode expected = tokenIndex == null
                ? question.path("analysisTargets").path(resolvedTargetIndex)
                : findQuestionWord(question, tokenIndex);
        if (!expected.isObject()) {
            throw new IllegalArgumentException("요청한 분석 대상 위치가 올바르지 않습니다.");
        }
        String storedText = tokenIndex == null
                ? expected.path("text").asText()
                : expected.path("surface").asText();
        if (!expectedText.equals(storedText)) {
            throw new IllegalArgumentException("요청한 텍스트가 생성된 훈련 문항과 일치하지 않습니다.");
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
            TrainingEntity training,
            int questionNumber,
            TrainingRecordingRequest request,
            RecordingTarget target,
            PronunciationWordAligner.Alignment alignment,
            boolean gazeRequired,
            int retryCount
    ) {
        List<StoredPronunciationWord> values = new ArrayList<>();
        for (PronunciationWordAligner.AlignedWord aligned : alignment.words()) {
            PronunciationReferenceWord reference = aligned.reference();
            var analyzed = aligned.analyzed();
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
                    training.getId(),
                    questionNumber,
                    target.targetIndex(),
                    reference.tokenIndex()
            );
            int speechStartOffsetMs = analyzed.offsetMs();
            int speechEndOffsetMs = analyzed.offsetMs() + analyzed.durationMs();
            WordAttemptLogEntity attempt = new WordAttemptLogEntity(
                    student,
                    word,
                    training,
                    reference.surface(),
                    true,
                    null,
                    null,
                    null,
                    null,
                    analyzed.isOmission(),
                    gazeRequired ? 0 : null,
                    pronunciationAccuracyScore,
                    speechStartOffsetMs,
                    speechEndOffsetMs,
                    correct,
                    totalScore,
                    questionNumber,
                    target.targetIndex(),
                    reference.tokenIndex(),
                    true
            );
            values.add(new StoredPronunciationWord(reference, analyzed, attempt));
        }
        List<WordAttemptLogEntity> saved = wordAttemptLogRepository.saveAllAndFlush(
                values.stream().map(StoredPronunciationWord::attempt).toList()
        );
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

    private void recordAttemptLinks(
            TrainingEntity training,
            int questionNumber,
            TrainingRecordingRequest request,
            RecordingTarget target,
            List<StoredPronunciationWord> storedWords,
            PronunciationAnalysisResult analysis,
            int insertionCount,
            int attemptNo,
            boolean passed,
            boolean questionCompleted
    ) {
        ObjectNode result = readObjectOrNew(training.getResult());
        result.put("schemaVersion", 2);
        ArrayNode attempts = result.withArray("wordAttempts");
        for (StoredPronunciationWord stored : storedWords) {
            Integer tokenIndex = stored.reference().tokenIndex();
            attempts.forEach(existing -> {
                if (existing.path("questionNo").asInt() == questionNumber
                        && existing.path("targetIndex").asInt() == target.targetIndex()
                        && java.util.Objects.equals(
                        nullableInt(existing, "tokenIndex"),
                        tokenIndex
                )) {
                    ((ObjectNode) existing).put("isFinal", false);
                }
            });
            ObjectNode link = attempts.addObject();
            link.put("wordAttemptLogId", stored.attempt().getId());
            link.put("questionNo", questionNumber);
            link.put("targetIndex", target.targetIndex());
            if (tokenIndex == null) link.putNull("tokenIndex");
            else link.put("tokenIndex", tokenIndex);
            link.put("isFinal", true);
            link.put("expectedText", stored.reference().surface());
            link.put(
                    "pronunciationAccuracyScore",
                    stored.analyzed().scoreOrZero()
            );
            link.put("pronunciationErrorType", stored.analyzed().errorType());
            link.put("pronunciationAnalysisVersion", analysis.analysisVersion());
            link.put("wordReadTimeMs", stored.analyzed().durationMs());
        }
        ObjectNode analysisLink = result.withArray("pronunciationAnalyses").addObject();
        analysisLink.put("questionNo", questionNumber);
        analysisLink.put("targetIndex", target.targetIndex());
        analysisLink.put("referenceText", target.referenceText());
        analysisLink.put(
                "pronunciationAccuracyScore",
                analysis.pronunciationAccuracyScore()
        );
        putNullableScore(analysisLink, "fluencyScore", analysis.fluencyScore());
        putNullableScore(analysisLink, "completenessScore", analysis.completenessScore());
        putNullableScore(analysisLink, "pronScore", analysis.pronScore());
        analysisLink.put("confidence", analysis.confidence());
        analysisLink.put("analysisVersion", analysis.analysisVersion());
        analysisLink.put("insertionCount", insertionCount);
        analysisLink.put("attemptNo", attemptNo);
        analysisLink.put("passed", passed);
        analysisLink.put("questionCompleted", questionCompleted);
        training.recordProgressResult(writeJson(result));
    }

    private void putNullableScore(ObjectNode node, String field, Double value) {
        if (value == null) node.putNull(field);
        else node.put(field, value);
    }

    private TrainingRecordingResponse.WordResult toRecordingWordResult(
            StoredPronunciationWord stored
    ) {
        return new TrainingRecordingResponse.WordResult(
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

    private void markPreviousAttemptsNotFinal(
            Long trainingId,
            int questionNumber,
            int targetIndex,
            Integer tokenIndex
    ) {
        wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndTargetIndexAndFinalAttemptTrue(
                        trainingId,
                        questionNumber,
                        targetIndex
                )
                .stream()
                .filter(attempt -> java.util.Objects.equals(attempt.getTokenIndex(), tokenIndex))
                .forEach(WordAttemptLogEntity::markNotFinal);
    }

    private void markPreviousSelectionNotFinal(Long trainingId, int questionNumber) {
        wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndFinalAttemptTrue(
                        trainingId,
                        questionNumber
                )
                .stream()
                .filter(attempt -> attempt.getTargetIndex() == null
                        && attempt.getTokenIndex() == null)
                .forEach(WordAttemptLogEntity::markNotFinal);
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
            com.iread.backend.pronunciation.PronunciationWordResult analyzed,
            WordAttemptLogEntity attempt
    ) {
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
        return learningQuestionSupport.toStudentQuestion(question);
    }

    private JsonNode findQuestion(Long trainingId, int questionNumber) {
        JsonNode generated = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다."));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray() || questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        return questions.get(questionNumber - 1);
    }

    private int questionCount(Long trainingId) {
        JsonNode generated = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다."));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        return questions.size();
    }

    private JsonNode findSubmission(ArrayNode submissions, UUID submissionId) {
        for (JsonNode submission : submissions) {
            if (submissionId.toString().equals(submission.path("submissionId").asText())) {
                return submission;
            }
        }
        return null;
    }

    private int countQuestionSubmissions(ArrayNode submissions, int questionNumber) {
        int count = 0;
        for (JsonNode submission : submissions) {
            if (submission.path("questionNo").asInt() == questionNumber) {
                count++;
            }
        }
        return count;
    }

    private int countQuestionPronunciationAnalyses(
            ObjectNode result,
            int questionNumber
    ) {
        int count = 0;
        for (JsonNode analysis : result.withArray("pronunciationAnalyses")) {
            if (analysis.path("questionNo").asInt() == questionNumber) {
                count++;
            }
        }
        return count;
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

    private boolean isQuestionCompleted(ObjectNode result, int questionNumber) {
        for (JsonNode question : result.withArray("questions")) {
            if (question.path("questionNo").asInt() == questionNumber) {
                return question.path("isCorrect").asBoolean(false)
                        || question.path("correctionConfirmed").asBoolean(false);
            }
        }
        return false;
    }

    private void upsertQuestionResult(
            ObjectNode result,
            int questionNumber,
            boolean correct,
            int totalScore,
            UUID submissionId,
            boolean correctionConfirmed
    ) {
        ArrayNode questions = result.withArray("questions");
        for (int index = questions.size() - 1; index >= 0; index--) {
            if (questions.get(index).path("questionNo").asInt() == questionNumber) {
                questions.remove(index);
            }
        }
        ObjectNode question = questions.addObject();
        question.put("questionNo", questionNumber);
        question.put("isCorrect", correct);
        question.put("totalScore", totalScore);
        question.put("submissionId", submissionId.toString());
        question.put("correctionConfirmed", correctionConfirmed);
    }

    private Set<Integer> completedQuestionNumbers(ObjectNode result) {
        Set<Integer> completed = new HashSet<>();
        result.withArray("questions").forEach(question -> {
            if (question.path("questionNo").asInt() > 0
                    && (question.path("isCorrect").asBoolean(false)
                    || question.path("correctionConfirmed").asBoolean(false))) {
                completed.add(question.path("questionNo").asInt());
            }
        });
        result.withArray("pronunciationAnalyses").forEach(analysis -> {
            if (analysis.path("questionCompleted").asBoolean(false)
                    && analysis.path("questionNo").asInt() > 0) {
                completed.add(analysis.path("questionNo").asInt());
            }
        });
        return completed;
    }

    private String hint(int attemptNo) {
        return attemptNo < MAX_SELECTION_ATTEMPTS
                ? "문항을 천천히 다시 살펴보세요."
                : "정답을 확인하고 정답과 똑같이 다시 해보세요.";
    }

    private ObjectNode writeFeedback(TrainingFeedbackResponse feedback) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("feedbackType", feedback.feedbackType());
        node.put("submissionId", feedback.submissionId().toString());
        node.put("attemptNo", feedback.attemptNo());
        node.put("maxAttempts", feedback.maxAttempts());
        node.put("remainingAttempts", feedback.remainingAttempts());
        node.put("correct", feedback.correct());
        node.put("questionCompleted", feedback.questionCompleted());
        node.put("canRetry", feedback.canRetry());
        ArrayNode locations = node.putArray("errorLocations");
        for (LearningErrorLocation location : feedback.errorLocations()) {
            ObjectNode item = locations.addObject();
            if (location.targetIndex() == null) item.putNull("targetIndex");
            else item.put("targetIndex", location.targetIndex());
            if (location.tokenIndex() == null) item.putNull("tokenIndex");
            else item.put("tokenIndex", location.tokenIndex());
            item.put("errorCode", location.errorCode());
        }
        if (feedback.hint() == null) node.putNull("hint");
        else node.put("hint", feedback.hint());
        if (feedback.correctResponse() == null) node.putNull("correctResponse");
        else node.set("correctResponse", feedback.correctResponse().deepCopy());
        return node;
    }

    private TrainingFeedbackResponse readFeedback(JsonNode node) {
        List<LearningErrorLocation> locations = new ArrayList<>();
        node.path("errorLocations").forEach(location -> locations.add(new LearningErrorLocation(
                nullableInt(location, "targetIndex"),
                nullableInt(location, "tokenIndex"),
                location.path("errorCode").asText()
        )));
        return new TrainingFeedbackResponse(
                node.path("feedbackType").asText(),
                UUID.fromString(node.path("submissionId").asText()),
                node.path("attemptNo").asInt(),
                node.path("maxAttempts").asInt(),
                node.path("remainingAttempts").asInt(),
                node.path("correct").asBoolean(),
                node.path("questionCompleted").asBoolean(),
                node.path("canRetry").asBoolean(),
                List.copyOf(locations),
                node.path("hint").isNull() ? null : node.path("hint").asText(),
                node.path("correctResponse").isNull()
                        ? null
                        : node.path("correctResponse").deepCopy()
        );
    }
}
