package com.iread.backend.training.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.app.dto.res.DemoLearningDateResponse;
import com.iread.backend.training.app.dto.res.DemoLearningCheatResponse;
import com.iread.backend.training.app.dto.res.DemoTrainingAdvanceResponse;
import com.iread.backend.training.config.DemoTrainingProgressResetService;
import com.iread.backend.training.curriculum.ActiveCurriculumPolicy;
import com.iread.backend.training.curriculum.CurriculumGenerationWorker;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.CurriculumReviewStatus;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoLearningCheatService {

    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository curriculumRepository;
    private final PersonalizedCurriculumPlanner curriculumPlanner;
    private final CurriculumGenerationWorker generationWorker;
    private final DemoTrainingProgressResetService resetService;
    private final TrainingRepository trainingRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final DemoLearningClock learningClock;

    @Transactional(readOnly = true)
    public DemoLearningDateResponse currentDate(Long teacherId, Long studentId) {
        requireOwnedStudent(teacherId, studentId);
        return new DemoLearningDateResponse(learningClock.currentDate(studentId));
    }

    @Transactional
    public DemoLearningCheatResponse resetProgress(Long teacherId, Long studentId) {
        requireOwnedStudent(teacherId, studentId);
        Long curriculumId = resetService.reset(studentId);
        DailyCurriculumEntity curriculum = curriculumRepository
                .findByIdAndStudentId(curriculumId, studentId)
                .orElseThrow();
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                curriculumId,
                "RESET"
        );
        return response(
                "RESET_PROGRESS",
                curriculum,
                learningClock.baseDate()
        );
    }

    @Transactional
    public DemoLearningCheatResponse advanceToNextDay(Long teacherId, Long studentId) {
        StudentEntity student = requireOwnedStudent(teacherId, studentId);
        // ActiveCurriculumPolicy는 교수자 검토 대기(REVIEW_REQUIRED) 커리큘럼을 제외하므로,
        // 검토 게이트까지 건너뛰어야 하는 데모 치트는 NOT_STARTED 원본을 폴백으로 집는다.
        DailyCurriculumEntity active = ActiveCurriculumPolicy
                .find(curriculumRepository, studentId)
                .or(() -> curriculumRepository.findByStudentIdAndStatus(
                        studentId,
                        DailyCurriculumStatus.NOT_STARTED
                ))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "진행 가능한 데모 커리큘럼을 찾을 수 없습니다."
                ));

        // 아직 손대지 않은 다음 회차(검사 추천 포함)가 이미 준비되어 있으면 그것을
        // 소모(강제 완료)하지 않고 플레이 가능 상태로 만들어 반환한다.
        boolean untouched = active.getStatus() == DailyCurriculumStatus.NOT_STARTED
                && active.getTrainings().stream().noneMatch(TrainingEntity::isCompleted);
        if (untouched) {
            generationWorker.generate(active.getId());
            DailyCurriculumEntity prepared = curriculumRepository
                    .findByIdAndStudentId(active.getId(), studentId)
                    .orElseThrow();
            // 데모 치트는 교수자 검토 게이트도 통과시킨다(정식 경로는 교수자 웹의 검수 완료).
            if (prepared.getReviewStatus() == CurriculumReviewStatus.REVIEW_REQUIRED) {
                prepared.completeReview(
                        student.getTeacher(),
                        learningClock.currentDateTime(studentId)
                );
            }
            realtimeEventPublisher.publishAfterCommit(
                    teacherId,
                    studentId,
                    RealtimeResource.CURRICULUM,
                    prepared.getId(),
                    "ADVANCED_TO_NEXT_DAY"
            );
            return response(
                    "ADVANCE_TO_NEXT_DAY",
                    prepared,
                    learningClock.currentDate(studentId)
            );
        }

        LocalDateTime completedAt = learningClock.nextDateTime(studentId);
        active.getTrainings().forEach(training -> {
            if (!training.isCompleted()) {
                training.complete(
                        "{\"cheat\":true,\"action\":\"ADVANCE_DAY\"}",
                        BigDecimal.valueOf(100),
                        completedAt
                );
            }
        });
        DailyCurriculumEntity next = curriculumPlanner.createNextIfAbsent(student);
        generationWorker.generate(next.getId());
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                next.getId(),
                "ADVANCED_TO_NEXT_DAY"
        );
        return response("ADVANCE_TO_NEXT_DAY", next, completedAt.toLocalDate());
    }

    @Transactional
    public DemoTrainingAdvanceResponse advanceToNextTraining(
            Long teacherId,
            Long studentId,
            Long trainingId
    ) {
        requireOwnedStudent(teacherId, studentId);
        TrainingEntity current = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "훈련을 찾을 수 없습니다."
                ));
        if (!current.isCompletable() && !current.isCompleted()) {
            throw new ConflictException("준비되지 않은 훈련은 강제 완료할 수 없습니다.");
        }

        TrainingEntity next = current.getDailyCurriculum().getTrainings().stream()
                .filter(training -> training.getSequenceNo() != null)
                .filter(training -> training.getSequenceNo() > current.getSequenceNo())
                .min(Comparator.comparingInt(TrainingEntity::getSequenceNo))
                .orElseThrow(() -> new ConflictException("다음 훈련이 없습니다."));
        if (next.getStatus() == TrainingStatus.COMPLETED) {
            throw new ConflictException("다음 훈련이 이미 완료되었습니다.");
        }

        LocalDateTime completedAt = learningClock.currentDateTime(studentId);
        if (!current.isCompleted()) {
            if (current.getStartedAt() == null) {
                current.start(completedAt);
            }
            current.complete(
                    "{\"cheat\":true,\"action\":\"ADVANCE_TRAINING\"}",
                    BigDecimal.valueOf(100),
                    completedAt
            );
        }
        if (next.getStatus() == TrainingStatus.NOT_READY) {
            next.markReady();
        }

        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                current.getId(),
                "COMPLETED"
        );

        return new DemoTrainingAdvanceResponse(
                "ADVANCE_TO_NEXT_TRAINING",
                current.getDailyCurriculum().getId(),
                current.getDailyCurriculum().getStatus().name(),
                current.getId(),
                current.getStatus().name(),
                next.getId(),
                next.getStatus().name(),
                learningClock.currentDate(studentId)
        );
    }

    private StudentEntity requireOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "학습자를 찾을 수 없습니다."
                ));
    }

    private DemoLearningCheatResponse response(
            String action,
            DailyCurriculumEntity curriculum,
            LocalDate currentDate
    ) {
        return new DemoLearningCheatResponse(
                action,
                curriculum.getId(),
                curriculum.getStatus().name(),
                curriculum.getTrainings().size(),
                currentDate
        );
    }
}
