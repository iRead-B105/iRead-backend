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

    @Query("""
            SELECT MAX(training.finishedAt)
            FROM TrainingEntity training
            WHERE training.dailyCurriculum.student.id = :studentId
              AND training.status = com.iread.backend.training.domain.TrainingStatus.COMPLETED
            """)
    Optional<LocalDateTime> findLatestFinishedAtByStudentId(@Param("studentId") Long studentId);

    @Query(value = """
            SELECT template.id AS trainingTemplateId,
                   template.name AS trainingTemplateName,
                   COUNT(training.id) AS completedCount
              FROM trainings training
              JOIN daily_curriculums curriculum
                ON curriculum.id = training.daily_curriculum_id
              JOIN training_templates template
                ON template.id = training.training_template_id
             WHERE curriculum.student_id = :studentId
               AND training.status = 'COMPLETED'
             GROUP BY template.id, template.name
             ORDER BY template.id
            """, nativeQuery = true)
    List<TrainingProgressProjection> findCompletedTrainingProgress(@Param("studentId") Long studentId);

    interface TrainingProgressProjection {
        Long getTrainingTemplateId();
        String getTrainingTemplateName();
        Long getCompletedCount();
    }
}
