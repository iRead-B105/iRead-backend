package com.iread.backend.training.repository;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyCurriculumRepository extends JpaRepository<DailyCurriculumEntity, Long> {
    @EntityGraph(attributePaths = {"trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    List<DailyCurriculumEntity> findAllByStudentIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    Optional<DailyCurriculumEntity> findByIdAndStudentId(Long id, Long studentId);
}
