package com.iread.backend.test.repository;

import com.iread.backend.test.domain.TestCurriculumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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

    boolean existsByStudentId(Long studentId);

    List<TestCurriculumEntity> findAllByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);

    List<TestCurriculumEntity> findAllByStudentIdAndStatusOrderByCreatedAtDescIdDesc(
            Long studentId,
            String status
    );

    Optional<TestCurriculumEntity> findByIdAndStudentId(Long id, Long studentId);

    Optional<TestCurriculumEntity> findByIdAndStudentIdAndStatus(
            Long id,
            Long studentId,
            String status
    );

    Optional<TestCurriculumEntity> findFirstByStudentIdAndStatusOrderByCreatedAtDescIdDesc(
            Long studentId,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select curriculum from TestCurriculumEntity curriculum where curriculum.id = :id")
    Optional<TestCurriculumEntity> findByIdForUpdate(@Param("id") Long id);
}
