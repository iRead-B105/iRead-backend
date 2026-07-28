package com.iread.backend.readingfeature.repository;

import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentFeatureProfileRepository extends JpaRepository<StudentFeatureProfileEntity, Long> {

    Optional<StudentFeatureProfileEntity> findByStudentIdAndReadingFeatureId(
            Long studentId,
            Long readingFeatureId
    );

    List<StudentFeatureProfileEntity> findAllByStudentIdOrderByWeaknessScoreDesc(Long studentId);

    @Query("select coalesce(max(profile.id), 0) from StudentFeatureProfileEntity profile")
    long findMaxId();
}
