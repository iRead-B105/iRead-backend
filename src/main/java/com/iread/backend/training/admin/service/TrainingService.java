package com.iread.backend.training.admin.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.analysis.GazeWordAnalysisAdapter;
import com.iread.backend.gaze.analysis.GazeWordAnalysisRequest;
import com.iread.backend.gaze.analysis.GazeWordAnalysisResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.*;
import com.iread.backend.training.curriculum.ActiveCurriculumPolicy;
import com.iread.backend.training.completion.TrainingCompletionAfterCommitPublisher;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.res.*;
import com.iread.backend.training.admin.result.TrainingQuestionResultAssembler;
import com.iread.backend.training.repository.*;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {
    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingTemplateRepository trainingTemplateRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final GazeWordAnalysisAdapter gazeWordAnalysisAdapter;
    private final AiClient aiClient;
    private final PersonalizedTrainingGenerationService personalizedTrainingGenerationService;
    private final TrainingCompletionAfterCommitPublisher completionFollowUpPublisher;
    private final com.iread.backend.training.curriculum.CurriculumGenerationAfterCommitTrigger
            curriculumGenerationTrigger;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final TrainingQuestionResultAssembler trainingQuestionResultAssembler;

    public List<CurriculumLogResponse> getCurriculumLogs(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        validateStudentOwner(teacherId, studentId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTimeExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        return dailyCurriculumRepository.findCompletedByStudentIdWithin(
                        studentId,
                        fromDateTime,
                        toDateTimeExclusive
                )
                .stream().map(curriculum -> new CurriculumLogResponse(
                        curriculum.getId(), curriculum.getCompletedAt().toLocalDate(), averageAccuracy(curriculum),
                        curriculum.getTrainings().stream().map(this::toCurriculumLogItem).toList()
                )).toList();
    }

    public TrainingLogResponse getTrainingLog(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        return new TrainingLogResponse(curriculum.getTrainings().stream().map(training -> {
            List<TrainingLogResponse.TrainingQuestionResult> questions =
                    trainingQuestionResultAssembler.assembleAll(training);
            return new TrainingLogResponse.TrainingItem(
                        training.getId(), training.getTrainingTemplate().getName(), training.getStartedAt(),
                        training.getFinishedAt(), toLegacyQuestionResults(questions),
                        training.getAccuracy(), toLegacyIncorrectItems(questions), questions
                );
        }).toList());
    }

    public TrainingStatisticsResponse getStatistics(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        List<TrainingStatisticsResponse.TrainingItem> items = curriculum.getTrainings().stream().map(current -> {
            TrainingEntity previous = previousTraining(studentId, current);
            return new TrainingStatisticsResponse.TrainingItem(
                    current.getId(), current.getTrainingTemplate().getName(), dateOf(current), current.getAccuracy(),
                    previous == null ? null : dateOf(previous), previous == null ? null : previous.getAccuracy()
            );
        }).toList();
        return new TrainingStatisticsResponse(items);
    }

    public List<TrainingCatalogResponse> getTrainingCatalog(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        Map<Long, BigDecimal> achievements = trainingRepository
                .findAllByDailyCurriculumStudentIdAndStatus(studentId, TrainingStatus.COMPLETED)
                .stream()
                .filter(training -> training.getAccuracy() != null)
                .collect(Collectors.toMap(
                        training -> training.getTrainingTemplate().getId(),
                        TrainingEntity::getAccuracy,
                        BigDecimal::max
                ));
        return trainingTemplateRepository
                .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc().stream()
                .filter(TrainingCatalogPolicy::isSelectable)
                .map(template -> new TrainingCatalogResponse(
                        template.getId(), template.getCurriculumUnit().getUnitName(), template.getSequenceNo(),
                        template.getName(), achievements.get(template.getId())
                )).toList();
    }

    public DailyCurriculumResponse getDailyCurriculum(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        return toDailyCurriculumResponse(curriculum);
    }

    public DailyCurriculumResponse getCurrentDailyCurriculum(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findByStudentIdAndStatus(studentId, DailyCurriculumStatus.NOT_STARTED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NEXT_CURRICULUM_NOT_FOUND",
                        "수정 가능한 커리큘럼을 찾을 수 없습니다."
                ));
        return getDailyCurriculum(teacherId, studentId, curriculum.getId());
    }

    public DailyCurriculumResponse getActiveDailyCurriculum(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = ActiveCurriculumPolicy
                .find(dailyCurriculumRepository, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACTIVE_CURRICULUM_NOT_FOUND",
                        "아동의 활성 커리큘럼을 찾을 수 없습니다."
                ));
        return getDailyCurriculum(teacherId, studentId, curriculum.getId());
    }

    @Transactional
    public DailyCurriculumResponse createDailyCurriculum(
            Long teacherId,
            Long studentId,
            UpdateCurriculumRequest request
    ) {
        StudentEntity student = studentRepository.findByIdAndTeacherIdForUpdate(
                        studentId,
                        teacherId
                )
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        if (dailyCurriculumRepository
                .findByStudentIdAndStatus(studentId, DailyCurriculumStatus.NOT_STARTED)
                .isPresent()) {
            throw new ConflictException("수정 가능한 커리큘럼은 한 개만 생성할 수 있습니다.");
        }

        List<Long> ids = request.trainingTemplateIds();
        List<TrainingTemplateEntity> templates = resolveTemplates(ids);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository.saveAndFlush(
                new DailyCurriculumEntity(student, templates)
        );
        // 교수자가 훈련마다 재생성을 누르지 않도록 커밋 직후 전체 교안을 백그라운드 생성한다.
        curriculumGenerationTrigger.generateAfterCommit(curriculum.getId());
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                curriculum.getId(),
                "CREATED"
        );
        return toDailyCurriculumResponse(curriculum);
    }

    @Transactional
    public void updateDailyCurriculum(Long teacherId, Long studentId, Long curriculumId,
        UpdateCurriculumRequest request) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findForUpdate(curriculumId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "일일 커리큘럼을 찾을 수 없습니다."
                ));
        if (curriculum.getStatus() != DailyCurriculumStatus.NOT_STARTED
                || curriculum.getTrainings().stream().anyMatch(training -> !training.isEditable())) {
            throw new ConflictException("진행 중이거나 완료된 커리큘럼은 수정할 수 없습니다.");
        }
        List<Long> ids = request.trainingTemplateIds();
        List<TrainingTemplateEntity> templates = resolveTemplates(ids);

        curriculum.getTrainings().forEach(t -> trainingDataRepository.deleteByTrainingId(t.getId()));
        trainingDataRepository.flush();
        curriculum.getTrainings().clear();
        dailyCurriculumRepository.flush();
        curriculum.replaceTrainings(templates);
        dailyCurriculumRepository.flush();
        // 구성 변경으로 비워진 교안도 커밋 직후 백그라운드에서 다시 채운다.
        curriculumGenerationTrigger.generateAfterCommit(curriculumId);
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                curriculumId,
                "UPDATED"
        );
    }

    @Transactional
    public CurriculumReviewResponse completeCurriculumReview(
            Long teacherId,
            Long studentId,
            Long curriculumId
    ) {
        StudentEntity student = validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findForUpdate(curriculumId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CURRICULUM_NOT_FOUND",
                        "Curriculum was not found."
                ));
        if (!curriculum.isRecommendedFromTest()) {
            throw new ConflictException("Only test-recommended curricula require final review.");
        }
        if (curriculum.getReviewStatus() == CurriculumReviewStatus.REVIEW_COMPLETED) {
            return toCurriculumReviewResponse(curriculum);
        }
        validateReviewableContent(curriculum);
        curriculum.completeReview(student.getTeacher(), LocalDateTime.now());
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                curriculumId,
                "REVIEW_COMPLETED"
        );
        return toCurriculumReviewResponse(curriculum);
    }

    public TrainingDetailResponse getTrainingDetail(
            Long teacherId,
            Long studentId,
            Long trainingId
    ) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        JsonNode generatedData = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::parseNullableObject)
                .orElse(null);
        return new TrainingDetailResponse(
                training.getId(),
                training.getTrainingTemplate().getId(),
                training.getTrainingTemplate().getName(),
                parseObject(training.getTrainingTemplate().getPrompt()),
                generatedData,
                training.getStatus().name().toLowerCase(Locale.ROOT),
                training.getStartedAt(),
                training.getFinishedAt(),
                parseNullableObject(training.getResult()),
                training.getAccuracy()
        );
    }

    public TrainingExportFile exportTraining(
            Long teacherId,
            Long studentId,
            Long trainingId,
            String format
    ) {
        String normalizedFormat = format == null
                ? "" : format.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("json", "csv").contains(normalizedFormat)) {
            throw new IllegalArgumentException("내보내기 형식은 csv 또는 json이어야 합니다.");
        }
        TrainingDetailResponse detail = getTrainingDetail(teacherId, studentId, trainingId);
        byte[] content = normalizedFormat.equals("json")
                ? writeBytes(detail)
                : toCsv(detail).getBytes(StandardCharsets.UTF_8);
        return new TrainingExportFile(
                "training-" + trainingId + "." + normalizedFormat,
                content
        );
    }

    @Transactional
    public JsonNode generateTraining(Long teacherId, Long studentId, Long trainingId) {
        validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRAINING_NOT_FOUND", "Training was not found."
                ));
        validateEditable(training);

        training.getDailyCurriculum().markRegenerationRequired();
        TrainingDataEntity data = findOrCreateTrainingData(training);
        ObjectNode previousData = parseObject(data.getGeneratedData());
        ObjectNode generatedData = personalizedTrainingGenerationService.generate(training);

        generatedData.put("revision", Math.max(0, previousData.path("revision").asInt(0)) + 1);
        data.updateGeneratedData(writeJson(generatedData));
        training.markReady();
        training.getDailyCurriculum().refreshReviewRequirement();
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "CONTENT_UPDATED"
        );
        return generatedData;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BigDecimal completeTraining(
            Long teacherId,
            Long studentId,
            Long trainingId,
            JsonNode result,
            LocalDateTime completedAt
    ) {
        TrainingEvaluationContext context = Objects.requireNonNull(
                transactionTemplate.execute(status -> prepareTrainingEvaluation(
                        teacherId, studentId, trainingId, result
                ))
        );
        if (context.completedAccuracy() != null) {
            return context.completedAccuracy();
        }

        String requestId = "training-evaluation-" + trainingId;
        EvaluateTrainingResponse response = aiClient.evaluateTraining(new EvaluateTrainingRequest(
                requestId,
                trainingId,
                studentId,
                context.trainingTemplateId(),
                1,
                context.finalResult()
        ));
        BigDecimal accuracy = response.accuracy().setScale(2, RoundingMode.HALF_UP);
        return Objects.requireNonNull(transactionTemplate.execute(status ->
                finishTrainingEvaluation(
                        teacherId,
                        studentId,
                        trainingId,
                        context.finalResult(),
                        accuracy,
                        completedAt
                )
        ));
    }

    private TrainingEvaluationContext prepareTrainingEvaluation(
            Long teacherId,
            Long studentId,
            Long trainingId,
            JsonNode result
    ) {
        validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
        if (training.isCompleted()) {
            return new TrainingEvaluationContext(training.getAccuracy(), null, null);
        }
        if (!training.isCompletable()) {
            throw new ConflictException("준비되지 않은 훈련은 완료할 수 없습니다.");
        }
        if (result == null || !result.isObject()) {
            throw new IllegalArgumentException("훈련 결과는 JSON 객체여야 합니다.");
        }
        trainingInputRequirementService.validateCompletion(trainingId);
        ObjectNode finalResult = (ObjectNode) result.deepCopy();
        if (training.getResult() != null && !training.getResult().isBlank()) {
            JsonNode progressAttempts = parseObject(training.getResult()).path("wordAttempts");
            if (progressAttempts.isArray()) {
                finalResult.set("wordAttempts", progressAttempts.deepCopy());
            }
        }
        return new TrainingEvaluationContext(
                null,
                training.getTrainingTemplate().getId(),
                finalResult
        );
    }

    private BigDecimal finishTrainingEvaluation(
            Long teacherId,
            Long studentId,
            Long trainingId,
            ObjectNode finalResult,
            BigDecimal accuracy,
            LocalDateTime completedAt
    ) {
        StudentEntity student = validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
        if (training.isCompleted()) {
            return training.getAccuracy();
        }
        if (!training.isCompletable()) {
            throw new ConflictException("준비되지 않은 훈련은 완료할 수 없습니다.");
        }

        LocalDateTime finishedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        if (training.getStartedAt() == null) {
            training.start(finishedAt);
        }
        saveWordAttemptLogs(student, training, finalResult.path("wordAttempts"));
        training.complete(writeJson(finalResult), accuracy, finishedAt);
        activateNextTraining(training);
        completionFollowUpPublisher.processAfterCommit(
                studentId,
                training.getDailyCurriculum().getStatus() == DailyCurriculumStatus.COMPLETED
        );
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "COMPLETED"
        );
        return accuracy;
    }

    private record TrainingEvaluationContext(
            BigDecimal completedAccuracy,
            Long trainingTemplateId,
            ObjectNode finalResult
    ) {
    }

    private void activateNextTraining(TrainingEntity completedTraining) {
        Integer completedSequence = completedTraining.getSequenceNo();
        if (completedSequence == null) {
            return;
        }
        completedTraining.getDailyCurriculum().getTrainings().stream()
                .filter(training -> training.getSequenceNo() != null)
                .filter(training -> training.getSequenceNo() > completedSequence)
                .min(Comparator.comparingInt(TrainingEntity::getSequenceNo))
                .filter(TrainingEntity::isEditable)
                .filter(training -> trainingDataRepository.findByTrainingId(training.getId()).isPresent())
                .ifPresent(TrainingEntity::markReady);
    }

    private StudentEntity validateStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private void validateEditable(TrainingEntity training) {
        if (!training.isEditable()) {
            throw new ConflictException("진행 중이거나 완료된 훈련은 수정할 수 없습니다.");
        }
    }

    private void saveWordAttemptLogs(StudentEntity student, TrainingEntity training, JsonNode attempts) {
        if (!attempts.isArray() || attempts.isEmpty()) {
            return;
        }

        Map<String, WordEntity> words = new HashMap<>();
        List<PendingAttemptLog> pendingLogs = new ArrayList<>();
        for (JsonNode attempt : attempts) {
            if (attempt.hasNonNull("wordAttemptLogId")) {
                continue;
            }
            String surfaceText = requiredSurfaceText(attempt);
            boolean hasAudioData = attempt.path("hasAudioData").asBoolean(false);
            boolean hasGazeData = attempt.path("hasGazeData").asBoolean(false);
            Boolean skipped = nullableBoolean(attempt, "isSkipped");
            Boolean correct = nullableBoolean(attempt, "isCorrect");
            Integer fixationDurationMs = nullableInteger(attempt, "fixationDurationMs");
            Integer fixationCount = nullableInteger(attempt, "fixationCount");
            Integer gazeStartOffsetMs = nullableInteger(attempt, "gazeStartOffsetMs");
            Integer gazeEndOffsetMs = nullableInteger(attempt, "gazeEndOffsetMs");
            Integer regressionCount = nullableInteger(attempt, "regressionCount");
            if (hasGazeData && fixationDurationMs == null
                    && fixationCount == null && regressionCount == null) {
                GazeWordAnalysisResult gaze = gazeWordAnalysisAdapter.analyze(
                        new GazeWordAnalysisRequest(
                                "training-gaze-" + training.getId() + "-" + pendingLogs.size(),
                                surfaceText,
                                gazeStartOffsetMs,
                                gazeEndOffsetMs
                        )
                );
                fixationDurationMs = gaze.fixationDurationMs();
                fixationCount = gaze.fixationCount();
                regressionCount = gaze.regressionCount();
                skipped = gaze.skipped();
                if (attempt instanceof ObjectNode objectAttempt) {
                    objectAttempt.put("fixationDurationMs", fixationDurationMs);
                    objectAttempt.put("fixationCount", fixationCount);
                    objectAttempt.put("regressionCount", regressionCount);
                    objectAttempt.put("isSkipped", skipped);
                    objectAttempt.put("gazeAnalysisConfidence", gaze.confidence());
                    objectAttempt.put("gazeAnalysisVersion", gaze.analysisVersion());
                }
            }
            Integer retryCount = nullableInteger(attempt, "retryCount");
            if (retryCount == null) {
                retryCount = regressionCount;
            }
            if (regressionCount == null) {
                regressionCount = retryCount;
            }
            Integer pronunciationAccuracyScore =
                    nullableInteger(attempt, "pronunciationAccuracyScore");
            if (pronunciationAccuracyScore == null
                    && attempt.hasNonNull("pronunciationScore")) {
                pronunciationAccuracyScore =
                        (int) Math.round(attempt.path("pronunciationScore").asDouble() * 10);
            }
            if (retryCount != null && retryCount < 0) {
                throw new IllegalArgumentException("단어 재응시 횟수는 0 이상이어야 합니다.");
            }
            Integer questionNo = nullableInteger(attempt, "questionNo");
            var requiredInputs = questionNo == null
                    ? java.util.Set.<TrainingInputType>of()
                    : trainingInputRequirementService.inputsForQuestion(
                            training.getId(),
                            questionNo
                    );
            boolean pronunciationRequired = requiredInputs.isEmpty()
                    ? hasAudioData
                    : requiredInputs.contains(TrainingInputType.VOICE);
            boolean gazeRequired = requiredInputs.isEmpty()
                    ? hasGazeData
                    : requiredInputs.contains(TrainingInputType.GAZE);
            Integer totalScore = wordAttemptScoreCalculator.calculate(
                    pronunciationAccuracyScore,
                    pronunciationRequired,
                    hasAudioData,
                    skipped,
                    gazeRequired,
                    hasGazeData,
                    skipped,
                    regressionCount,
                    retryCount,
                    correct
            );
            WordEntity word = words.computeIfAbsent(surfaceText, text ->
                    wordRepository.findByContent(text)
                            .orElseGet(() -> wordRepository.save(new WordEntity(text))));
            WordAttemptLogEntity log = new WordAttemptLogEntity(
                    student,
                    word,
                    training,
                    surfaceText,
                    hasGazeData,
                    hasAudioData,
                    fixationDurationMs,
                    fixationCount,
                    gazeStartOffsetMs,
                    gazeEndOffsetMs,
                    skipped,
                    regressionCount,
                    pronunciationAccuracyScore,
                    nullableInteger(attempt, "speechStartOffsetMs"),
                    nullableInteger(attempt, "speechEndOffsetMs"),
                    correct,
                    totalScore,
                    questionNo,
                    nullableInteger(attempt, "targetIndex"),
                    nullableInteger(attempt, "tokenIndex"),
                    attempt.path("isFinal").asBoolean(true)
            );
            if (attempt instanceof ObjectNode objectAttempt) {
                if (!objectAttempt.has("isFinal")) {
                    objectAttempt.put("isFinal", true);
                }
                pendingLogs.add(new PendingAttemptLog(objectAttempt, log));
            }
        }
        if (pendingLogs.isEmpty()) {
            return;
        }
        List<WordAttemptLogEntity> saved = wordAttemptLogRepository.saveAllAndFlush(
                pendingLogs.stream().map(PendingAttemptLog::log).toList()
        );
        for (int index = 0; index < saved.size(); index++) {
            pendingLogs.get(index).attempt().put("wordAttemptLogId", saved.get(index).getId());
        }
    }

    private record PendingAttemptLog(ObjectNode attempt, WordAttemptLogEntity log) {
    }

    private String requiredSurfaceText(JsonNode attempt) {
        String surfaceText = nullableText(attempt, "surfaceText", 50);
        if (surfaceText == null) {
            throw new IllegalArgumentException("단어 시도 로그의 surfaceText는 필수입니다.");
        }
        return surfaceText;
    }

    private String nullableText(JsonNode node, String field, int maxLength) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        String value = node.path(field).asText().trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + "의 길이는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return value;
    }

    private Integer nullableInteger(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asInt() : null;
    }

    private Boolean nullableBoolean(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asBoolean() : null;
    }

    private DailyCurriculumEntity findCurriculum(Long studentId, Long curriculumId) {
        return dailyCurriculumRepository.findByIdAndStudentId(curriculumId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "일일 커리큘럼을 찾을 수 없습니다."
                ));
    }

    private TrainingEntity findOwnedTraining(Long teacherId, Long studentId, Long trainingId) {
        validateStudentOwner(teacherId, studentId);
        return trainingRepository.findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
    }

    private List<TrainingTemplateEntity> resolveTemplates(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        Map<Long, TrainingTemplateEntity> templates = trainingTemplateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TrainingTemplateEntity::getId, Function.identity()));
        if (templates.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("존재하지 않는 훈련 템플릿이 있습니다.");
        }
        if (templates.values().stream().anyMatch(template -> !TrainingCatalogPolicy.isSelectable(template))) {
            throw new IllegalArgumentException("더 이상 제공하지 않는 훈련 템플릿이 포함되어 있습니다.");
        }
        return ids.stream().map(templates::get).toList();
    }

    private DailyCurriculumResponse toDailyCurriculumResponse(DailyCurriculumEntity curriculum) {
        return new DailyCurriculumResponse(
                curriculum.getId(),
                curriculum.getStatus().name(),
                curriculum.getSourceTestCurriculum() == null
                        ? null : curriculum.getSourceTestCurriculum().getId(),
                curriculum.getReviewStatus().name(),
                curriculum.getReviewedByTeacher() == null
                        ? null : curriculum.getReviewedByTeacher().getId(),
                curriculum.getReviewedAt(),
                curriculum.getTrainings().stream()
                .map(training -> new DailyCurriculumResponse.TrainingItem(
                        training.getId(),
                        training.getTrainingTemplate().getId(),
                        training.getSequenceNo(),
                        training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                        training.getTrainingTemplate().getName(),
                        training.getStatus().name()
                ))
                .toList()
        );
    }

    private void validateReviewableContent(DailyCurriculumEntity curriculum) {
        if (curriculum.getStatus() != DailyCurriculumStatus.NOT_STARTED) {
            throw new ConflictException("A started curriculum cannot be reviewed.");
        }
        if (curriculum.getReviewStatus() != CurriculumReviewStatus.REVIEW_REQUIRED) {
            throw new ConflictException("Curriculum content is not ready for final review.");
        }
        if (curriculum.getTrainings().size() != PersonalizedCurriculumPlanner.TRAINING_COUNT) {
            throw new ConflictException("A reviewed curriculum must contain exactly five trainings.");
        }
        for (TrainingEntity training : curriculum.getTrainings()) {
            if (training.getStatus() != TrainingStatus.NOT_STARTED) {
                throw new ConflictException("Every training must be generated before final review.");
            }
            ObjectNode generated = trainingDataRepository.findByTrainingId(training.getId())
                    .map(TrainingDataEntity::getGeneratedData)
                    .map(this::parseObject)
                    .orElseThrow(() -> new ConflictException(
                            "Every training must have saved generated content before final review."
                    ));
            if (!generated.path("questions").isArray()
                    || generated.path("questions").isEmpty()) {
                throw new ConflictException(
                        "Every training must have saved questions before final review."
                );
            }
        }
    }

    private CurriculumReviewResponse toCurriculumReviewResponse(
            DailyCurriculumEntity curriculum
    ) {
        return new CurriculumReviewResponse(
                curriculum.getId(),
                curriculum.getReviewStatus().name(),
                curriculum.getReviewedByTeacher() == null
                        ? null : curriculum.getReviewedByTeacher().getId(),
                curriculum.getReviewedAt()
        );
    }

    private BigDecimal averageAccuracy(DailyCurriculumEntity curriculum) {
        List<BigDecimal> values = curriculum.getTrainings().stream().map(TrainingEntity::getAccuracy)
                .filter(Objects::nonNull).toList();
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private CurriculumLogResponse.TrainingItem toCurriculumLogItem(TrainingEntity training) {
        return new CurriculumLogResponse.TrainingItem(training.getId(),
                training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                training.getTrainingTemplate().getName());
    }

    private TrainingEntity previousTraining(Long studentId, TrainingEntity current) {
        if (current.getFinishedAt() == null) return null;
        List<TrainingEntity> records = trainingRepository
                .findAllByDailyCurriculumStudentIdAndTrainingTemplateIdAndStatusAndFinishedAtLessThanOrderByFinishedAtDesc(
                        studentId, current.getTrainingTemplate().getId(), TrainingStatus.COMPLETED, current.getFinishedAt());
        return records.isEmpty() ? null : records.getFirst();
    }

    private LocalDate dateOf(TrainingEntity training) {
        return training.getFinishedAt() == null ? null : training.getFinishedAt().toLocalDate();
    }

    private List<TrainingLogResponse.QuestionResult> toLegacyQuestionResults(
            List<TrainingLogResponse.TrainingQuestionResult> questions
    ) {
        return questions.stream().map(question -> new TrainingLogResponse.QuestionResult(
                question.questionNo(),
                question.correct()
        )).toList();
    }

    private List<TrainingLogResponse.IncorrectItem> toLegacyIncorrectItems(
            List<TrainingLogResponse.TrainingQuestionResult> questions
    ) {
        return questions.stream()
                .filter(question -> Boolean.FALSE.equals(question.correct()))
                .map(question -> new TrainingLogResponse.IncorrectItem(
                        question.questionNo(),
                        legacyText(question.question()),
                        legacyText(question.correctAnswer()),
                        legacyText(question.selectedAnswer())
                ))
                .toList();
    }

    private String legacyText(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isObject() && value.size() == 1 && value.path("text").isTextual()) {
            return value.path("text").asText();
        }
        return value.toString();
    }

    private TrainingDataEntity findOrCreateTrainingData(TrainingEntity training) {
        return trainingDataRepository.findByTrainingId(training.getId())
                .orElseGet(() -> trainingDataRepository.save(new TrainingDataEntity(training,
                        "{\"version\":1,\"content\":{}}")));
    }

    private ObjectNode trainingDataRoot(TrainingEntity training, boolean required) {
        Optional<TrainingDataEntity> data = trainingDataRepository.findByTrainingId(training.getId());
        if (data.isEmpty()) {
            if (required) {
                throw new ResourceNotFoundException("훈련 데이터를 찾을 수 없습니다.");
            }
            return null;
        }
        return parseObject(data.get().getGeneratedData());
    }

    private ObjectNode parseObject(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node instanceof ObjectNode object) return object;
            throw new IllegalArgumentException("JSON 객체 형식이 필요합니다.");
        } catch (Exception exception) {
            throw new IllegalArgumentException("저장된 훈련 데이터 형식이 올바르지 않습니다.");
        }
    }

    private JsonNode parseNullableObject(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return parseObject(json);
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 결과 파일 생성에 실패했습니다.");
        }
    }

    private String toCsv(TrainingDetailResponse detail) {
        return String.join(",",
                "trainingId",
                "trainingTemplateId",
                "name",
                "status",
                "startedAt",
                "finishedAt",
                "accuracy",
                "result"
        ) + System.lineSeparator() + String.join(",",
                csv(detail.trainingId()),
                csv(detail.trainingTemplateId()),
                csv(detail.name()),
                csv(detail.status()),
                csv(detail.startedAt()),
                csv(detail.finishedAt()),
                csv(detail.accuracy()),
                csv(detail.result())
        ) + System.lineSeparator();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString()
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private ObjectNode validateGeneratedData(JsonNode generatedData) {
        if (!(generatedData instanceof ObjectNode root)) {
            throw new IllegalArgumentException("AI가 생성한 훈련 데이터는 JSON 객체여야 합니다.");
        }
        JsonNode questions = root.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new IllegalArgumentException("AI가 생성한 훈련 데이터에는 questions가 한 개 이상 필요합니다.");
        }
        Set<String> questionIds = new HashSet<>();
        for (JsonNode question : questions) {
            String questionId = question.path("questionId").asText();
            if (questionId.isBlank() || !questionIds.add(questionId)) {
                throw new IllegalArgumentException("각 문제에는 중복되지 않는 questionId가 필요합니다.");
            }
            if (!question.path("problem").isObject()) {
                throw new IllegalArgumentException("각 문제에는 problem JSON 객체가 필요합니다.");
            }
            if (!question.path("answer").isObject()) {
                throw new IllegalArgumentException("각 문제에는 answer JSON 객체가 필요합니다.");
            }
        }
        return root.deepCopy();
    }

    private String writeJson(JsonNode node) {
        try { return objectMapper.writeValueAsString(node); }
        catch (Exception exception) { throw new IllegalStateException("훈련 데이터 저장에 실패했습니다."); }
    }
}
