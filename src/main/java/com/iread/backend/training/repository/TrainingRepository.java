package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TrainingRepository extends JpaRepository<TrainingEntity, Long> {
    @EntityGraph(attributePaths = {"trainingTemplate", "dailyCurriculum"})
    Optional<TrainingEntity> findByIdAndDailyCurriculumStudentId(Long id, Long studentId);

    @EntityGraph(attributePaths = {"trainingTemplate"})
    List<TrainingEntity> findAllByDailyCurriculumStudentIdAndStatus(Long studentId, TrainingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"trainingTemplate", "dailyCurriculum"})
    @Query("""
            SELECT training
            FROM TrainingEntity training
            WHERE training.id = :trainingId
              AND training.dailyCurriculum.student.id = :studentId
            """)
    Optional<TrainingEntity> findForUpdate(
            @Param("trainingId") Long trainingId,
            @Param("studentId") Long studentId
    );

    List<TrainingEntity> findAllByDailyCurriculumStudentIdAndTrainingTemplateIdAndStatusAndFinishedAtLessThanOrderByFinishedAtDesc(
            Long studentId, Long templateId, TrainingStatus status, java.time.LocalDateTime finishedAt
    );

    List<TrainingEntity> findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
            Long studentId, TrainingStatus status, LocalDateTime start, LocalDateTime end
    );
}
