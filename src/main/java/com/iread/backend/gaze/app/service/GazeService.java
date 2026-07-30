package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.analysis.GazeWordMetricMergeService;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.GazeAnalysisDetailResponse;
import com.iread.backend.gaze.app.dto.res.GazeCalibrationGuideResponse;
import com.iread.backend.gaze.app.dto.res.GazeDeviceStatusResponse;
import com.iread.backend.gaze.app.dto.res.GazeSessionResponse;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GazeService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TrainingRepository trainingRepository;
    private final StoryRepository storyRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final GazeWordMetricMergeService gazeWordMetricMergeService;
    private final WordAttemptLogRepository wordAttemptLogRepository;

    public GazeDeviceStatusResponse getDeviceStatus(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return new GazeDeviceStatusResponse(
                true,
                "Web Eye Tracker",
                "READY",
                "Eye tracker is ready."
        );
    }

    public GazeCalibrationGuideResponse getCalibrationGuide(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return new GazeCalibrationGuideResponse(
                true,
                "Look at the center point to calibrate the eye tracker."
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
            throw new IllegalArgumentException("endStatus must be COMPLETED or FAILED.");
        }
        if (request.endStatus() == GazeSessionStatus.COMPLETED
                && !hasCompletedData(request.data())) {
            throw new IllegalArgumentException("Completed gaze sessions require sample or word gaze data.");
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

    public GazeAnalysisDetailResponse getTestGazeAnalysis(Long teacherId, Long studentId, Long testId) {
        validateStudentOwner(teacherId, studentId);
        findOwnedTest(studentId, testId);
        GazeSessionEntity session = gazeSessionRepository
                .findFirstByStudentIdAndTestIdAndStatusOrderByEndedAtDescIdDesc(
                        studentId,
                        testId,
                        GazeSessionStatus.COMPLETED
                )
                .orElseThrow(() -> new ResourceNotFoundException("Gaze analysis result was not found."));
        return toAnalysisDetailResponse(
                session,
                wordAttemptLogRepository.findAllByTestIdAndFinalAttemptTrueOrderByIdAsc(testId)
        );
    }

    public GazeAnalysisDetailResponse getTrainingGazeAnalysis(Long teacherId, Long studentId, Long trainingId) {
        validateStudentOwner(teacherId, studentId);
        findOwnedTraining(studentId, trainingId);
        GazeSessionEntity session = gazeSessionRepository
                .findFirstByStudentIdAndTrainingIdAndStatusOrderByEndedAtDescIdDesc(
                        studentId,
                        trainingId,
                        GazeSessionStatus.COMPLETED
                )
                .orElseThrow(() -> new ResourceNotFoundException("Gaze analysis result was not found."));
        return toAnalysisDetailResponse(
                session,
                wordAttemptLogRepository.findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(trainingId)
        );
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

    private void validateContentReference(StartGazeSessionRequest request) {
        int referenceCount = (request.testId() == null ? 0 : 1)
                + (request.trainingId() == null ? 0 : 1)
                + (request.storyId() == null ? 0 : 1);
        if (referenceCount != 1) {
            throw new IllegalArgumentException("Exactly one content reference is required.");
        }
        boolean matchingReference = switch (request.contentType()) {
            case TEST -> request.testId() != null;
            case TRAINING -> request.trainingId() != null;
            case STORY -> request.storyId() != null;
        };
        if (!matchingReference) {
            throw new IllegalArgumentException("contentType does not match the provided reference id.");
        }
    }

    private void requireRunning(GazeSessionEntity gazeSession) {
        if (gazeSession.getStatus() != GazeSessionStatus.RUNNING) {
            throw new ConflictException("Only running gaze sessions can be ended.");
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

    private GazeAnalysisDetailResponse toAnalysisDetailResponse(
            GazeSessionEntity session,
            List<WordAttemptLogEntity> attempts
    ) {
        GazeMetricSummary summary = summarizeGazeMetrics(attempts)
                .orElseThrow(() -> new ResourceNotFoundException("Gaze analysis result was not found."));
        return new GazeAnalysisDetailResponse(
                session.getId(),
                null,
                summary.totalVisitedDuration(),
                summary.totalVisitedCount(),
                summary.reverseReadCount(),
                summary.avgVisitedDuration()
        );
    }

    private Optional<GazeMetricSummary> summarizeGazeMetrics(List<WordAttemptLogEntity> attempts) {
        List<WordAttemptLogEntity> gazeAttempts = attempts.stream()
                .filter(this::hasGazeMetric)
                .toList();
        if (gazeAttempts.isEmpty()) {
            return Optional.empty();
        }
        int totalDuration = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getFixationDurationMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int totalCount = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getFixationCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int reverseReadCount = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getRegressionCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int avgVisitedDuration = totalCount == 0 ? 0 : totalDuration / totalCount;
        return Optional.of(new GazeMetricSummary(
                totalDuration,
                totalCount,
                reverseReadCount,
                avgVisitedDuration
        ));
    }

    private boolean hasGazeMetric(WordAttemptLogEntity attempt) {
        return attempt.getFixationDurationMs() != null
                || attempt.getFixationCount() != null
                || attempt.getRegressionCount() != null
                || attempt.getGazeStartOffsetMs() != null
                || attempt.getGazeEndOffsetMs() != null;
    }

    private GazeSessionEntity findOwnedGazeSessionForUpdate(Long gazeSessionId, Long studentId) {
        return gazeSessionRepository.findByIdAndStudentIdForUpdate(gazeSessionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Gaze session was not found."));
    }

    private StudentTestEntity findOwnedTest(Long studentId, Long testId) {
        if (testId == null) {
            throw new IllegalArgumentException("testId is required.");
        }
        StudentTestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test was not found."));
        if (!studentId.equals(test.getStudent().getId())) {
            throw new ResourceNotFoundException("Test was not found.");
        }
        return test;
    }

    private TrainingEntity findOwnedTraining(Long studentId, Long trainingId) {
        if (trainingId == null) {
            throw new IllegalArgumentException("trainingId is required.");
        }
        return trainingRepository.findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Training was not found."));
    }

    private StoryEntity findOwnedStory(Long studentId, Long storyId) {
        if (storyId == null) {
            throw new IllegalArgumentException("storyId is required.");
        }
        return storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Story was not found."));
    }

    private StudentEntity findStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Student was not found."));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        findStudentOwner(teacherId, studentId);
    }

    private record GazeMetricSummary(
            int totalVisitedDuration,
            int totalVisitedCount,
            int reverseReadCount,
            int avgVisitedDuration
    ) {
    }
}
