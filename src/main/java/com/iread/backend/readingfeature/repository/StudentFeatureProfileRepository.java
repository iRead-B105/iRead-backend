package com.iread.backend.readingfeature.repository;

import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentFeatureProfileRepository extends JpaRepository<StudentFeatureProfileEntity, Long> {

    Optional<StudentFeatureProfileEntity> findByStudentIdAndReadingFeatureId(
            Long studentId,
            Long readingFeatureId
    );

    List<StudentFeatureProfileEntity> findAllByStudentIdOrderByWeaknessScoreDesc(Long studentId);
}
