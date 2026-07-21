package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {
    @EntityGraph(attributePaths = {"trainingTemplate", "dailyCurriculum"})
    Optional<TrainingEntity> findByIdAndDailyCurriculumStudentId(Long id, Long studentId);

    List<TrainingEntity> findAllByDailyCurriculumStudentIdAndTrainingTemplateIdAndStatusAndFinishedAtLessThanOrderByFinishedAtDesc(
            Long studentId, Long templateId, TrainingStatus status, java.time.LocalDateTime finishedAt
    );
}
