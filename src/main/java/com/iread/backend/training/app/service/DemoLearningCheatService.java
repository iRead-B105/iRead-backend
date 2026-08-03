package com.iread.backend.training.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.app.dto.res.DemoLearningCheatResponse;
import com.iread.backend.training.app.dto.res.DemoTrainingAdvanceResponse;
import com.iread.backend.training.config.DemoTrainingProgressResetService;
import com.iread.backend.training.curriculum.ActiveCurriculumPolicy;
import com.iread.backend.training.curriculum.CurriculumGenerationWorker;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional
    public DemoLearningCheatResponse resetProgress(Long teacherId, Long studentId) {
        requireOwnedStudent(teacherId, studentId);
        Long curriculumId = resetService.reset(studentId);
        DailyCurriculumEntity curriculum = curriculumRepository
                .findByIdAndStudentId(curriculumId, studentId)
                .orElseThrow();
        return response("RESET_PROGRESS", curriculum);
    }

    @Transactional
    public DemoLearningCheatResponse advanceToNextDay(Long teacherId, Long studentId) {
        StudentEntity student = requireOwnedStudent(teacherId, studentId);
        DailyCurriculumEntity active = ActiveCurriculumPolicy
                .find(curriculumRepository, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "진행 가능한 데모 커리큘럼을 찾을 수 없습니다."
                ));
        LocalDateTime completedAt = LocalDateTime.now();
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
        return response("ADVANCE_TO_NEXT_DAY", next);
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

        LocalDateTime completedAt = LocalDateTime.now();
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

        return new DemoTrainingAdvanceResponse(
                "ADVANCE_TO_NEXT_TRAINING",
                current.getDailyCurriculum().getId(),
                current.getDailyCurriculum().getStatus().name(),
                current.getId(),
                current.getStatus().name(),
                next.getId(),
                next.getStatus().name()
        );
    }

    private StudentEntity requireOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "학습자를 찾을 수 없습니다."
                ));
    }

    private DemoLearningCheatResponse response(String action, DailyCurriculumEntity curriculum) {
        return new DemoLearningCheatResponse(
                action,
                curriculum.getId(),
                curriculum.getStatus().name(),
                curriculum.getTrainings().size()
        );
    }
}
