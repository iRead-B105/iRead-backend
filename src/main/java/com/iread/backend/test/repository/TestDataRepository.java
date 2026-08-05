package com.iread.backend.test.repository;

import com.iread.backend.test.domain.TestDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestDataRepository extends JpaRepository<TestDataEntity, Long> {
    Optional<TestDataEntity> findFirstByTestIdOrderByCreatedAtDescIdDesc(Long testId);
}
