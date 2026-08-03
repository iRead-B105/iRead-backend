package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.app.dto.res.LearningEntryResponse;
import com.iread.backend.student.app.dto.res.LearningEntryStatus;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningEntryService {
    static final int TOTAL_CHALLENGE_QUESTIONS = 9;

    private final StudentRepository studentRepository;
    private final TestCurriculumRepository testCurriculumRepository;
    private final StudentTestRepository studentTestRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;

    public LearningEntryResponse getLearningEntry(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        if (testCurriculumRepository.existsByStudentIdAndStatus(
                studentId,
                TestStatus.COMPLETED.name()
        )) {
            return home(studentId);
        }

        if (dailyCurriculumRepository.existsByStudentId(studentId)) {
            return home(studentId);
        }

        var inProgress = testCurriculumRepository
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(
                        studentId,
                        List.of(
                                TestStatus.NOT_STARTED.name(),
                                TestStatus.IN_PROGRESS.name()
                        )
                );
        if (inProgress.isPresent()) {
            TestCurriculumEntity curriculum = inProgress.get();
            int completedQuestions = Math.toIntExact(
                    studentTestRepository.countByTestCurriculumIdAndStatus(
                            curriculum.getId(),
                            TestStatus.COMPLETED
                    )
            );
            return new LearningEntryResponse(
                    studentId,
                    LearningEntryStatus.CHALLENGE_IN_PROGRESS,
                    curriculum.getId(),
                    completedQuestions,
                    TOTAL_CHALLENGE_QUESTIONS
            );
        }

        return new LearningEntryResponse(
                studentId,
                LearningEntryStatus.CHALLENGE_REQUIRED,
                null,
                0,
                TOTAL_CHALLENGE_QUESTIONS
        );
    }

    private LearningEntryResponse home(Long studentId) {
        return new LearningEntryResponse(
                studentId,
                LearningEntryStatus.HOME,
                null,
                0,
                TOTAL_CHALLENGE_QUESTIONS
        );
    }
}
