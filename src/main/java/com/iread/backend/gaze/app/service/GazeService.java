package com.iread.backend.gaze.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.*;
import com.iread.backend.gaze.analysis.GazeWordMetricMergeService;
import com.iread.backend.gaze.domain.*;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.gaze.repository.WordAttemptLogRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GazeService {
    private final ObjectMapper objectMapper;
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TrainingRepository trainingRepository;
    private final StoryRepository storyRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final GazeWordMetricMergeService gazeWordMetricMergeService;
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
        return toSessionResponse(gazeSession);
    }

    @Transactional
    public GazeSessionResponse failSession(Long teacherId, Long gazeSessionId, FailGazeSessionRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        GazeSessionEntity gazeSession = findOwnedGazeSessionForUpdate(gazeSessionId, request.studentId());
        requireRunning(gazeSession);
        gazeSession.fail(LocalDateTime.now());
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
        requireRunning(gazeSession);
        gazeSession.end(
                request.endStatus(),
                LocalDateTime.now(),
                request.data() == null ? null : request.data().toString()
        );
        if (request.endStatus() == GazeSessionStatus.COMPLETED) {
            gazeWordMetricMergeService.merge(gazeSession, request.data());
        }
        return toSessionResponse(gazeSession);
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
        saveWordAttempts(result, request);

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
        if (request.sentenceMetrics() == null) {
            return;
        }
        if (gazeSession.getContentType() != GazeContentType.STORY) {
            throw new IllegalArgumentException("문장별 시선 지표는 이야기 세션에서만 저장할 수 있습니다.");
        }
        var combinedData = objectMapper.createObjectNode();
        if (gazeSession.getData() != null && !gazeSession.getData().isBlank()) {
            combinedData.set("rawData", objectMapper.readTree(gazeSession.getData()));
        }
        combinedData.set("sentenceMetrics", request.sentenceMetrics());
        gazeSession.updateData(combinedData.toString());
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

    private GazeAnalysisDetailResponse toAnalysisDetailResponse(GazeAnalysisResultEntity result) {
        return new GazeAnalysisDetailResponse(
                result.getGazeSession().getId(),
                result.getId(),
                result.getTotalVisitedDuration(),
                result.getTotalVisitedCount(),
                result.getReverseReadCount(),
                result.getAvgVisitedDuration(),
                result.getSentenceMetrics(),
                result.getRegressions(),
                result.getAnalysisMeta()
        );
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
