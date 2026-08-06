package com.iread.backend.test.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.admin.dto.res.TestCurriculumListResponse;
import com.iread.backend.test.admin.result.TestCurriculumResultAggregator;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCurriculumAdminService {
    private final StudentRepository studentRepository;
    private final TestCurriculumRepository testCurriculumRepository;
    private final StudentTestRepository studentTestRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TestCurriculumResultAggregator resultAggregator;

    public TestCurriculumListResponse getCurriculums(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return new TestCurriculumListResponse(testCurriculumRepository
                .findAllByStudentIdAndStatusOrderByCreatedAtDescIdDesc(
                        studentId,
                        TestStatus.COMPLETED.name()
                )
                .stream()
                .map(curriculum -> resultAggregator.summarize(
                        curriculum,
                        studentTestRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
                                curriculum.getId()
                        )
                ))
                .toList());
    }

    public TestCurriculumDetailResponse getCurriculum(
            Long teacherId,
            Long studentId,
            Long testCurriculumId
    ) {
        validateStudentOwner(teacherId, studentId);
        TestCurriculumEntity curriculum = testCurriculumRepository
                .findByIdAndStudentIdAndStatus(
                        testCurriculumId,
                        studentId,
                        TestStatus.COMPLETED.name()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "실력도전 검사 결과를 찾을 수 없습니다."
                ));
        return resultAggregator.aggregate(
                curriculum,
                studentTestRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
                        curriculum.getId()
                ),
                dailyCurriculumRepository.findBySourceTestCurriculumId(curriculum.getId())
                        .orElse(null)
        );
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }
}
