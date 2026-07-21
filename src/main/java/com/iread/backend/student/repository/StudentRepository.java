package com.iread.backend.student.repository;

import com.iread.backend.student.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    List<StudentEntity> findAllByTeacherIdOrderByIdAsc(Long teacherId);
    Optional<StudentEntity> findByIdAndTeacherId(Long id, Long teacherId);
    boolean existsByStudentCode(String studentCode);
    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);

    @Query(value = """
            SELECT s.id AS studentId,
                   MAX(t.finished_at) AS recentFinishedAt,
                   COALESCE(SUM(TIMESTAMPDIFF(SECOND, t.started_at, t.finished_at)) DIV 60, 0) AS totalLearningMinutes,
                   (SELECT tt2.name
                      FROM daily_curriculums dc2
                      JOIN trainings t2 ON t2.daily_curriculum_id = dc2.id
                      JOIN training_templates tt2 ON tt2.id = t2.training_template_id
                     WHERE dc2.student_id = s.id AND t2.status = 'COMPLETED'
                     ORDER BY t2.finished_at DESC, t2.id DESC
                     LIMIT 1) AS recentTrainingName
              FROM students s
              LEFT JOIN daily_curriculums dc ON dc.student_id = s.id
              LEFT JOIN trainings t ON t.daily_curriculum_id = dc.id AND t.status = 'COMPLETED'
             WHERE s.teacher_id = :teacherId
             GROUP BY s.id
            """, nativeQuery = true)
    List<StudentLearningSummaryProjection> findLearningSummaries(@Param("teacherId") Long teacherId);

    @Query(value = """
            SELECT DATE(t.finished_at) AS learningDate, AVG(t.accuracy) AS accuracy
              FROM daily_curriculums dc
              JOIN trainings t ON t.daily_curriculum_id = dc.id
             WHERE dc.student_id = :studentId
               AND t.status = 'COMPLETED'
               AND t.finished_at IS NOT NULL
             GROUP BY DATE(t.finished_at)
             ORDER BY DATE(t.finished_at)
            """, nativeQuery = true)
    List<AccuracyTrendProjection> findAccuracyTrend(@Param("studentId") Long studentId);

    @Query(value = """
            SELECT DATE(t.started_at) AS learningDate, tt.name AS learningType,
                   t.started_at AS startedAt, t.finished_at AS finishedAt, t.accuracy AS achievement
              FROM daily_curriculums dc
              JOIN trainings t ON t.daily_curriculum_id = dc.id
              JOIN training_templates tt ON tt.id = t.training_template_id
             WHERE dc.student_id = :studentId
             ORDER BY t.started_at DESC, t.id DESC
            """, nativeQuery = true)
    List<TrainingHistoryProjection> findTrainingHistory(@Param("studentId") Long studentId);

    @Modifying
    @Query(value = "DELETE t FROM trainings t JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id WHERE dc.student_id = :studentId", nativeQuery = true)
    void deleteTrainingsByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query(value = "DELETE FROM daily_curriculums WHERE student_id = :studentId", nativeQuery = true)
    void deleteDailyCurriculumsByStudentId(@Param("studentId") Long studentId);

    interface StudentLearningSummaryProjection {
        Long getStudentId();
        LocalDateTime getRecentFinishedAt();
        Long getTotalLearningMinutes();
        String getRecentTrainingName();
    }

    interface AccuracyTrendProjection {
        LocalDate getLearningDate();
        BigDecimal getAccuracy();
    }

    interface TrainingHistoryProjection {
        LocalDate getLearningDate();
        String getLearningType();
        LocalDateTime getStartedAt();
        LocalDateTime getFinishedAt();
        BigDecimal getAchievement();
    }
}
