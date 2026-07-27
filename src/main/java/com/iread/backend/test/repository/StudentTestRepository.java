package com.iread.backend.test.repository;

import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface StudentTestRepository extends JpaRepository<StudentTestEntity, Long> {
    List<StudentTestEntity> findAllByTestCurriculumStudentIdAndStatusOrderByCreatedAtDesc(
            Long studentId,
            TestStatus status
    );

    Optional<StudentTestEntity> findByIdAndTestCurriculumStudentIdAndStatus(
            Long id,
            Long studentId,
            TestStatus status
    );

    List<StudentTestEntity> findAllByIdInAndTestCurriculumStudentIdAndStatus(
            Collection<Long> ids, Long studentId, TestStatus status
    );

    List<StudentTestEntity> findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long studentId, TestStatus status, LocalDateTime start, LocalDateTime end
    );

    Optional<StudentTestEntity>
    findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
            Long studentId,
            Collection<TestStatus> statuses
    );

    Optional<StudentTestEntity> findByIdAndTestCurriculumStudentId(Long id, Long studentId);
}
