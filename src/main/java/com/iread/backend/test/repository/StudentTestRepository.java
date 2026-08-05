package com.iread.backend.test.repository;

import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface StudentTestRepository extends JpaRepository<StudentTestEntity, Long> {
    long countByTestCurriculumIdAndStatus(Long testCurriculumId, TestStatus status);

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

    List<StudentTestEntity> findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(
            Long testCurriculumId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT test
            FROM StudentTestEntity test
            WHERE test.id = :testId
              AND test.testCurriculum.student.id = :studentId
            """)
    Optional<StudentTestEntity> findByIdAndStudentIdForUpdate(
            @Param("testId") Long testId,
            @Param("studentId") Long studentId
    );
}
