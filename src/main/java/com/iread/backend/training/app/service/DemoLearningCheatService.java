package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.app.dto.res.DemoLearningCheatResponse;
import com.iread.backend.training.config.DemoTrainingProgressResetService;
import com.iread.backend.training.curriculum.ActiveCurriculumPolicy;
import com.iread.backend.training.curriculum.CurriculumGenerationWorker;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoLearningCheatService {

    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository curriculumRepository;
    private final PersonalizedCurriculumPlanner curriculumPlanner;
    private final CurriculumGenerationWorker generationWorker;
    private final DemoTrainingProgressResetService resetService;

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
