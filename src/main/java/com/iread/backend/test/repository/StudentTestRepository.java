package com.iread.backend.test.repository;

import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentTestRepository extends JpaRepository<StudentTestEntity, Long> {
    List<StudentTestEntity> findAllByStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, TestStatus status);

    Optional<StudentTestEntity> findByIdAndStudentIdAndStatus(Long id, Long studentId, TestStatus status);

    List<StudentTestEntity> findAllByIdInAndStudentIdAndStatus(
            Collection<Long> ids, Long studentId, TestStatus status
    );
}
