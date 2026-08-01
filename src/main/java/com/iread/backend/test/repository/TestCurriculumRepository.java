package com.iread.backend.test.repository;

import com.iread.backend.test.domain.TestCurriculumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestCurriculumRepository extends JpaRepository<TestCurriculumEntity, Long> {

    Optional<TestCurriculumEntity> findFirstByStudentIdOrderByCreatedAtDescIdDesc(
            Long studentId
    );

    Optional<TestCurriculumEntity> findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(
            Long studentId,
            Collection<String> statuses
    );

    boolean existsByStudentIdAndStatus(Long studentId, String status);

    List<TestCurriculumEntity> findAllByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);

    Optional<TestCurriculumEntity> findByIdAndStudentId(Long id, Long studentId);
}
