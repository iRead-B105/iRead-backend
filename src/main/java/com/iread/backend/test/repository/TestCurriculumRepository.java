package com.iread.backend.test.repository;

import com.iread.backend.test.domain.TestCurriculumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestCurriculumRepository extends JpaRepository<TestCurriculumEntity, Long> {

    Optional<TestCurriculumEntity> findFirstByStudentIdOrderByCreatedAtDescIdDesc(
            Long studentId
    );
}
