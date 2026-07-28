package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.*;
import com.iread.backend.gaze.domain.*;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GazeService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final TrainingRepository trainingRepository;
    private final StoryRepository storyRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;

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
        StudentTestEntity test = null;
        TrainingEntity training = null;
        StoryEntity story = null;

        switch (request.contentType()) {
            case TEST -> test = findOwnedTest(request.studentId(), request.testId());
            case TRAINING -> training = findOwnedTraining(request.studentId(), request.trainingId());
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
        GazeSessionEntity gazeSession = findOwnedGazeSession(gazeSessionId, request.studentId());
        gazeSession.fail(LocalDateTime.now());
        return toSessionResponse(gazeSession);
    }

    @Transactional
    public GazeSessionResponse endSession(Long teacherId, Long gazeSessionId, EndGazeSessionRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        if (request.status() != GazeSessionStatus.COMPLETED && request.status() != GazeSessionStatus.FAILED) {
            throw new IllegalArgumentException("종료 상태는 COMPLETED 또는 FAILED만 사용할 수 있습니다.");
        }
        GazeSessionEntity gazeSession = findOwnedGazeSession(gazeSessionId, request.studentId());
        gazeSession.end(request.status(), LocalDateTime.now());
        return toSessionResponse(gazeSession);
    }

    @Transactional
    public GazeAnalysisResultResponse saveAnalysisResult(Long teacherId, Long gazeSessionId,
                                                         GazeAnalysisResultRequest request) {
        validateStudentOwner(teacherId, request.studentId());
        GazeSessionEntity gazeSession = findOwnedGazeSession(gazeSessionId, request.studentId());

        GazeAnalysisResultEntity result = gazeAnalysisResultRepository.saveAndFlush(
                new GazeAnalysisResultEntity(
                        gazeSession,
                        request.totalVisitedDuration(),
                        request.totalVisitedCount(),
                        request.reverseReadCount(),
                        request.avgVisitedDuration()
                )
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

    private GazeSessionEntity findOwnedGazeSession(Long gazeSessionId, Long studentId) {
        return gazeSessionRepository.findByIdAndStudentId(gazeSessionId, studentId)
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
