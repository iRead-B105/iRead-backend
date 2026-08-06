package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.*;
import com.iread.backend.gaze.analysis.GazeWordMetricMergeService;
import com.iread.backend.gaze.analysis.GazeDepartureCounter;
import com.iread.backend.gaze.domain.*;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GazeService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TrainingRepository trainingRepository;
    private final StoryRepository storyRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final GazeDepartureCounter gazeDepartureCounter;
    private final GazeWordMetricMergeService gazeWordMetricMergeService;
    private final GazeDataStorage gazeDataStorage;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ObjectMapper objectMapper;

    public GazeDeviceStatusResponse getDeviceStatus(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return new GazeDeviceStatusResponse(
                true,
                "Web Eye Tracker",
                "READY",
                "시선 추적 장치를 사용할 수 있습니다."
        );
    }

    public GazeCalibrationGuideResponse getCalibrationGuide(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return new GazeCalibrationGuideResponse(
                true,
                "화면 중앙의 점을 바라보며 보정을 진행해 주세요."
        );
    }

    @Transactional
    public GazeSessionResponse startSession(Long teacherId, StartGazeSessionRequest request) {
        StudentEntity student = findStudentOwner(teacherId, request.studentId());
        validateContentReference(request);
        StudentTestEntity test = null;
        TrainingEntity training = null;
        StoryEntity story = null;

        switch (request.contentType()) {
            case TEST -> test = findOwnedTest(request.studentId(), request.testId());
            case TRAINING -> {
                training = findOwnedTraining(request.studentId(), request.trainingId());
                trainingInputRequirementService.requireTrainingInput(
                        training.getId(),
                        TrainingInputType.GAZE
                );
            }
            case STORY -> story = findOwnedStory(request.studentId(), request.storyId());
        }

        GazeSessionEntity gazeSession = gazeSessionRepository.saveAndFlush(new GazeSessionEntity(
                student,
                test,
                training,
                story,
                request.contentType(),
                request.calibrationStatus(),
                LocalDateTime.now()
        ));
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                request.studentId(),
                RealtimeResource.GAZE,
                gazeSession.getId(),
                "STARTED"
        );
        return toSessionResponse(gazeSession);
    }

    @Transactional
    public GazeSessionResponse failSession(Long teacherId, Long gazeSessionId, FailGazeSessionRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        GazeSessionEntity gazeSession = findOwnedGazeSessionForUpdate(gazeSessionId, request.studentId());
        requireRunning(gazeSession);
        gazeSession.fail(LocalDateTime.now());
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                request.studentId(),
                RealtimeResource.GAZE,
                gazeSessionId,
                "FAILED"
        );
        return toSessionResponse(gazeSession);
    }

    @Transactional
    public GazeSessionResponse endSession(Long teacherId, Long gazeSessionId, EndGazeSessionRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        if (request.endStatus() != GazeSessionStatus.COMPLETED
                && request.endStatus() != GazeSessionStatus.FAILED) {
            throw new IllegalArgumentException("종료 상태는 COMPLETED 또는 FAILED만 사용할 수 있습니다.");
        }
        if (request.endStatus() == GazeSessionStatus.COMPLETED
                && !hasCompletedData(request.data())) {
            throw new IllegalArgumentException(
                    "완료된 시선 세션에는 한 건 이상의 시선 샘플 또는 단어 지표가 필요합니다."
            );
        }
        GazeSessionEntity gazeSession = findOwnedGazeSessionForUpdate(gazeSessionId, request.studentId());
        // 같은 상태로의 종료 재요청은 멱등 처리한다. 1차 종료가 서버에 반영됐지만
        // 응답이 유실되면 학습 완료 재시도('다시 시도할래요')가 이 종료를 다시
        // 보내는데, 충돌로 거절하면 완료가 영영 막힌다.
        if (gazeSession.getStatus() == request.endStatus()) {
            return toSessionResponse(gazeSession);
        }
        requireRunning(gazeSession);
        if (request.endStatus() == GazeSessionStatus.COMPLETED) {
            gazeWordMetricMergeService.merge(gazeSession, request.data());
            updateTestGazeDepartureMetric(gazeSession, request.data());
        }
        String dataUrl = request.data() == null
                ? null
                : storeWithRollbackCleanup(
                        request.studentId(),
                        gazeSession.getId(),
                        request.data().toString()
                );
        gazeSession.end(request.endStatus(), LocalDateTime.now(), dataUrl);
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                request.studentId(),
                RealtimeResource.GAZE,
                gazeSessionId,
                request.endStatus().name()
        );
        return toSessionResponse(gazeSession);
    }

    private String storeWithRollbackCleanup(
            Long studentId,
            Long gazeSessionId,
            String rawData
    ) {
        String dataUrl = gazeDataStorage.store(studentId, gazeSessionId, rawData);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                                return;
                            }
                            try {
                                gazeDataStorage.delete(dataUrl);
                            } catch (RuntimeException cleanupFailure) {
                                log.error(
                                        "롤백된 시선 원시 파일을 삭제하지 못했습니다. dataUrl={}",
                                        dataUrl,
                                        cleanupFailure
                                );
                            }
                        }
                    }
            );
        }
        return dataUrl;
    }

    @Transactional
    public GazeAnalysisResultResponse saveAnalysisResult(Long teacherId, Long gazeSessionId,
                                                         GazeAnalysisResultRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        GazeSessionEntity gazeSession = findOwnedGazeSessionForUpdate(gazeSessionId, request.studentId());
        if (gazeSession.getStatus() != GazeSessionStatus.COMPLETED) {
            throw new ConflictException("완료된 시선 세션에만 분석 결과를 저장할 수 있습니다.");
        }
        if (gazeAnalysisResultRepository.existsByGazeSessionId(gazeSessionId)) {
            throw new ConflictException("시선 세션의 분석 결과가 이미 저장되어 있습니다.");
        }
        saveSentenceMetrics(gazeSession, request);

        GazeAnalysisResultEntity result = gazeAnalysisResultRepository.saveAndFlush(
                new GazeAnalysisResultEntity(
                        gazeSession,
                        resolveTotalVisitedDuration(request),
                        resolveTotalVisitedCount(request),
                        resolveReverseReadCount(request),
                        resolveAvgVisitedDuration(request),
                        toJson(request.sentenceMetrics()),
                        toJson(request.regressions()),
                        toJson(request.analysisMeta())
                )
        );
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                request.studentId(),
                RealtimeResource.GAZE,
                gazeSessionId,
                "ANALYSIS_AVAILABLE"
        );
        return new GazeAnalysisResultResponse(result.getId(), result.getCreatedAt());
    }

    public GazeAnalysisDetailResponse getTestGazeAnalysis(Long teacherId, Long studentId, Long testId) {
        validateStudentOwner(teacherId, studentId);
        findOwnedTest(studentId, testId);
        GazeAnalysisResultEntity result = gazeAnalysisResultRepository
                .findFirstByGazeSessionStudentIdAndGazeSessionTestIdOrderByCreatedAtDesc(studentId, testId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "시선 분석 결과를 찾을 수 없습니다."
                ));
        return toAnalysisDetailResponse(result);
    }

    public TestQuestionGazeAnalysisResponse getTestQuestionGazeAnalysis(
            Long teacherId,
            Long studentId,
            Long testId,
            Integer questionNo
    ) {
        validateStudentOwner(teacherId, studentId);
        findOwnedTest(studentId, testId);
        if (questionNo == null || questionNo < 1) {
            throw new IllegalArgumentException("questionNo must be a positive integer.");
        }
        GazeAnalysisResultEntity result = gazeAnalysisResultRepository
                .findFirstByGazeSessionStudentIdAndGazeSessionTestIdOrderByCreatedAtDesc(
                        studentId,
                        testId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gaze analysis result not found."
                ));
        List<WordAttemptLogEntity> attempts = wordAttemptLogRepository
                .findAllByTestIdAndQuestionNoAndFinalAttemptTrue(testId, questionNo)
                .stream()
                .filter(WordAttemptLogEntity::isHasGazeData)
                .toList();
        if (attempts.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Question-level gaze analysis result not found."
            );
        }

        List<TestQuestionGazeAnalysisResponse.WordMetric> wordMetrics = attempts.stream()
                .map(this::toQuestionWordMetric)
                .toList();
        int totalDwellTime = wordMetrics.stream()
                .mapToInt(TestQuestionGazeAnalysisResponse.WordMetric::dwellDurationMs)
                .sum();
        int dwellCount = wordMetrics.stream()
                .mapToInt(TestQuestionGazeAnalysisResponse.WordMetric::visitCount)
                .sum();
        int regressionCount = wordMetrics.stream()
                .mapToInt(TestQuestionGazeAnalysisResponse.WordMetric::regressionCount)
                .sum();
        Integer averageFixationTime = dwellCount == 0
                ? null
                : totalDwellTime / dwellCount;

        return new TestQuestionGazeAnalysisResponse(
                testId,
                questionNo,
                result.getGazeSession().getId(),
                result.getId(),
                totalDwellTime,
                dwellCount,
                regressionCount,
                averageFixationTime,
                wordMetrics,
                new TestQuestionGazeAnalysisResponse.AnalysisMeta(
                        "gaze-word-v1",
                        "BACKEND"
                )
        );
    }

    private TestQuestionGazeAnalysisResponse.WordMetric toQuestionWordMetric(
            WordAttemptLogEntity attempt
    ) {
        return new TestQuestionGazeAnalysisResponse.WordMetric(
                attempt.getTargetIndex(),
                attempt.getTokenIndex(),
                attempt.getSurfaceText(),
                valueOrZero(attempt.getFixationDurationMs()),
                valueOrZero(attempt.getFixationCount()),
                attempt.getSkipped(),
                valueOrZero(attempt.getRegressionCount()),
                attempt.getGazeStartOffsetMs(),
                attempt.getGazeEndOffsetMs()
        );
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public GazeAnalysisDetailResponse getTrainingGazeAnalysis(Long teacherId, Long studentId, Long trainingId) {
        validateStudentOwner(teacherId, studentId);
        findOwnedTraining(studentId, trainingId);
        GazeAnalysisResultEntity result = gazeAnalysisResultRepository
                .findFirstByGazeSessionStudentIdAndGazeSessionTrainingIdOrderByCreatedAtDesc(studentId, trainingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "시선 분석 결과를 찾을 수 없습니다."
                ));
        return toAnalysisDetailResponse(result);
    }

    private GazeSessionResponse toSessionResponse(GazeSessionEntity gazeSession) {
        return new GazeSessionResponse(
                gazeSession.getId(),
                gazeSession.getContentType(),
                gazeSession.getStatus(),
                gazeSession.getCalibrationStatus(),
                gazeSession.getStartedAt(),
                gazeSession.getEndedAt()
        );
    }

    private void saveSentenceMetrics(GazeSessionEntity gazeSession, GazeAnalysisResultRequest request) {
        if (request.sentenceMetrics() == null || request.sentenceMetrics().isEmpty()) {
            return;
        }
        if (gazeSession.getContentType() != GazeContentType.STORY) {
            throw new IllegalArgumentException("문장별 시선 지표는 이야기 세션에서만 저장할 수 있습니다.");
        }
        var combinedData = objectMapper.createObjectNode();
        String storedData = gazeSession.getDataUrl() == null
                ? null
                : gazeDataStorage.load(gazeSession.getDataUrl());
        if (storedData != null && !storedData.isBlank()) {
            combinedData.set("rawData", objectMapper.readTree(storedData));
        }
        combinedData.set("sentenceMetrics", objectMapper.valueToTree(request.sentenceMetrics()));
        if (gazeSession.getDataUrl() == null) {
            gazeSession.updateDataUrl(gazeDataStorage.store(
                    gazeSession.getStudent().getId(),
                    gazeSession.getId(),
                    combinedData.toString()
            ));
            return;
        }
        gazeDataStorage.overwrite(gazeSession.getDataUrl(), combinedData.toString());
    }

    private void validateContentReference(StartGazeSessionRequest request) {
        int referenceCount = (request.testId() == null ? 0 : 1)
                + (request.trainingId() == null ? 0 : 1)
                + (request.storyId() == null ? 0 : 1);
        if (referenceCount != 1) {
            throw new IllegalArgumentException("콘텐츠 식별자는 정확히 하나만 입력해야 합니다.");
        }
        boolean matchingReference = switch (request.contentType()) {
            case TEST -> request.testId() != null;
            case TRAINING -> request.trainingId() != null;
            case STORY -> request.storyId() != null;
        };
        if (!matchingReference) {
            throw new IllegalArgumentException("contentType과 콘텐츠 식별자가 일치하지 않습니다.");
        }
    }

    private void requireRunning(GazeSessionEntity gazeSession) {
        if (gazeSession.getStatus() != GazeSessionStatus.RUNNING) {
            throw new ConflictException("실행 중인 시선 세션만 종료할 수 있습니다.");
        }
    }

    private boolean hasCompletedData(tools.jackson.databind.JsonNode data) {
        if (data == null || data.isNull()) {
            return false;
        }
        if (data.isArray()) {
            return !data.isEmpty();
        }
        if (!data.isObject()) {
            return false;
        }
        return (data.path("samples").isArray() && !data.path("samples").isEmpty())
                || (data.path("words").isArray() && !data.path("words").isEmpty());
    }

    private void updateTestGazeDepartureMetric(
            GazeSessionEntity gazeSession,
            tools.jackson.databind.JsonNode data
    ) {
        if (gazeSession.getContentType() != GazeContentType.TEST
                || gazeSession.getTest() == null) {
            return;
        }
        Integer departureCount = gazeDepartureCounter.count(data);
        if (departureCount == null) {
            return;
        }
        Long studentId = gazeSession.getStudent().getId();
        StudentTestEntity test = testRepository.findByIdAndStudentIdForUpdate(
                        gazeSession.getTest().getId(),
                        studentId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Test not found."));
        ObjectNode result = objectMapper.createObjectNode();
        if (test.getResult() != null && !test.getResult().isBlank()) {
            tools.jackson.databind.JsonNode stored = objectMapper.readTree(test.getResult());
            if (stored != null && stored.isObject()) {
                result = (ObjectNode) stored.deepCopy();
            }
        }
        result.put("gazeDepartureCount", departureCount);
        test.updateResultMetrics(result.toString());
    }

    private GazeAnalysisDetailResponse toAnalysisDetailResponse(GazeAnalysisResultEntity result) {
        return new GazeAnalysisDetailResponse(
                result.getGazeSession().getId(),
                result.getId(),
                result.getTotalVisitedDuration(),
                result.getTotalVisitedCount(),
                result.getReverseReadCount(),
                result.getAvgVisitedDuration()
        );
    }

    private Integer resolveTotalVisitedDuration(GazeAnalysisResultRequest request) {
        if (request.totalVisitedDuration() != null) {
            return request.totalVisitedDuration();
        }
        if (request.wordAttempts() == null) {
            return 0;
        }
        return request.wordAttempts().stream()
                .map(GazeAnalysisResultRequest.WordAttempt::fixationDurationMs)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Integer resolveTotalVisitedCount(GazeAnalysisResultRequest request) {
        if (request.totalVisitedCount() != null) {
            return request.totalVisitedCount();
        }
        if (request.wordAttempts() == null) {
            return 0;
        }
        return request.wordAttempts().stream()
                .map(GazeAnalysisResultRequest.WordAttempt::fixationCount)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Integer resolveReverseReadCount(GazeAnalysisResultRequest request) {
        if (request.reverseReadCount() != null) {
            return request.reverseReadCount();
        }
        if (request.regressions() != null) {
            return request.regressions().size();
        }
        if (request.wordAttempts() == null) {
            return 0;
        }
        return request.wordAttempts().stream()
                .map(GazeAnalysisResultRequest.WordAttempt::regressionCount)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Integer resolveAvgVisitedDuration(GazeAnalysisResultRequest request) {
        if (request.avgVisitedDuration() != null) {
            return request.avgVisitedDuration();
        }
        Integer totalDuration = resolveTotalVisitedDuration(request);
        Integer totalCount = resolveTotalVisitedCount(request);
        if (totalCount == null || totalCount == 0) {
            return 0;
        }
        return totalDuration / totalCount;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "시선 분석 요청을 JSON으로 변환할 수 없습니다.",
                    exception
            );
        }
    }

    private GazeSessionEntity findOwnedGazeSessionForUpdate(Long gazeSessionId, Long studentId) {
        return gazeSessionRepository.findByIdAndStudentIdForUpdate(gazeSessionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "시선 트래킹 세션을 찾을 수 없습니다."
                ));
    }

    private StudentTestEntity findOwnedTest(Long studentId, Long testId) {
        if (testId == null) {
            throw new IllegalArgumentException("테스트 ID가 필요합니다.");
        }
        StudentTestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("테스트를 찾을 수 없습니다."));
        if (!studentId.equals(test.getStudent().getId())) {
            throw new ResourceNotFoundException("테스트를 찾을 수 없습니다.");
        }
        return test;
    }

    private TrainingEntity findOwnedTraining(Long studentId, Long trainingId) {
        if (trainingId == null) {
            throw new IllegalArgumentException("훈련 ID가 필요합니다.");
        }
        return trainingRepository.findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
    }

    private StoryEntity findOwnedStory(Long studentId, Long storyId) {
        if (storyId == null) {
            throw new IllegalArgumentException("스토리 ID가 필요합니다.");
        }
        return storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("스토리를 찾을 수 없습니다."));
    }

    private StudentEntity findStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        findStudentOwner(teacherId, studentId);
    }
}
