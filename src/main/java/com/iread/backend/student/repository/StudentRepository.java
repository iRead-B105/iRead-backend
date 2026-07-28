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
    long countByTeacherId(Long teacherId);

    @Query(value = """
            SELECT COUNT(DISTINCT dc.student_id)
              FROM daily_curriculums dc
              JOIN students s ON s.id = dc.student_id
             WHERE s.teacher_id = :teacherId
               AND DATE(dc.created_at) = CURRENT_DATE
               AND dc.status IN ('NOT_STARTED', 'IN_PROGRESS')
            """, nativeQuery = true)
    long countScheduledToday(@Param("teacherId") Long teacherId);

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
            SELECT DATE(t.finished_at) AS learningDate,
                   AVG(wal.total_score) AS averageScore
              FROM word_attempt_logs wal
              JOIN trainings t ON t.id = wal.training_id
              JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
             WHERE dc.student_id = :studentId
               AND wal.use_location = 'TRAINING'
               AND wal.total_score IS NOT NULL
               AND t.status = 'COMPLETED'
               AND t.finished_at IS NOT NULL
             GROUP BY DATE(t.finished_at)
             ORDER BY DATE(t.finished_at)
            """, nativeQuery = true)
    List<AccuracyTrendProjection> findAccuracyTrend(@Param("studentId") Long studentId);

    @Query(value = """
            SELECT t.id AS trainingId, DATE(t.started_at) AS learningDate, tt.name AS learningType,
                   cu.unit_name AS learningCategory,
                   t.started_at AS startedAt, t.finished_at AS finishedAt, t.accuracy AS achievement,
                   t.result AS result
              FROM daily_curriculums dc
              JOIN trainings t ON t.daily_curriculum_id = dc.id
              JOIN training_templates tt ON tt.id = t.training_template_id
              JOIN curriculum_units cu ON cu.id = tt.curriculum_unit_id
             WHERE dc.student_id = :studentId
               AND t.status = 'COMPLETED'
               AND t.finished_at IS NOT NULL
               AND (:fromDate IS NULL OR DATE(t.finished_at) >= :fromDate)
               AND (:toDate IS NULL OR DATE(t.finished_at) <= :toDate)
             ORDER BY t.finished_at DESC, t.id DESC
            """, nativeQuery = true)
    List<TrainingHistoryProjection> findTrainingHistory(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDate from,
            @Param("toDate") LocalDate to
    );

    @Query(value = """
            SELECT (
                       SELECT cu.unit_name
                         FROM trainings t
                         JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                         JOIN training_templates tt ON tt.id = t.training_template_id
                         JOIN curriculum_units cu ON cu.id = tt.curriculum_unit_id
                        WHERE dc.student_id = :studentId
                        ORDER BY COALESCE(t.finished_at, t.started_at, t.created_at) DESC, t.id DESC
                        LIMIT 1
                   ) AS currentStage,
                   (
                       SELECT MAX(event_at)
                         FROM (
                               SELECT MAX(t.finished_at) AS event_at
                                 FROM trainings t
                                 JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                                WHERE dc.student_id = :studentId AND t.status = 'COMPLETED'
                               UNION ALL
                               SELECT MAX(gs.ended_at)
                                 FROM gaze_sessions gs
                                WHERE gs.student_id = :studentId AND gs.status = 'COMPLETED'
                              ) learning_events
                   ) AS lastLearningAt,
                   (
                       SELECT COUNT(*)
                         FROM trainings t
                         JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                        WHERE dc.student_id = :studentId
                          AND t.status = 'COMPLETED'
                          AND t.finished_at >= CURRENT_TIMESTAMP - INTERVAL 6 WEEK
                   ) AS recentCompletedCount,
                   (
                       SELECT AVG(t.accuracy) / 10
                         FROM trainings t
                         JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                        WHERE dc.student_id = :studentId
                          AND t.status = 'COMPLETED'
                          AND t.finished_at >= CURRENT_TIMESTAMP - INTERVAL 6 WEEK
                          AND t.accuracy IS NOT NULL
                   ) AS recentAverageAccuracy,
                   (
                       SELECT COUNT(*)
                         FROM gaze_sessions gs
                        WHERE gs.student_id = :studentId
                          AND gs.id = (
                              SELECT latest_gs.id
                                FROM gaze_sessions latest_gs
                               WHERE latest_gs.student_id = :studentId
                               ORDER BY COALESCE(
                                            latest_gs.ended_at,
                                            latest_gs.created_at
                                        ) DESC,
                                        latest_gs.id DESC
                               LIMIT 1
                          )
                          AND gs.status = 'FAILED'
                          AND COALESCE(gs.ended_at, gs.created_at)
                              >= CURRENT_TIMESTAMP - INTERVAL 30 DAY
                   ) AS recentGazeFailureCount
            """, nativeQuery = true)
    LearningOverviewProjection findLearningOverview(@Param("studentId") Long studentId);

    @Query(value = """
            SELECT t.id AS eventId,
                   t.finished_at AS occurredAt,
                   t.accuracy / 10 AS accuracy,
                   COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(t.result, '$.retryCount')) AS SIGNED), 0)
                       AS retryCount,
                   GROUP_CONCAT(
                       DISTINCT CASE
                           WHEN wal.is_correct = FALSE OR wal.is_skipped = TRUE
                           THEN wal.surface_text
                       END
                       ORDER BY wal.id SEPARATOR '|||'
                   ) AS problemSegments
              FROM trainings t
              JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
              LEFT JOIN word_attempt_logs wal ON wal.training_id = t.id
             WHERE dc.student_id = :studentId
               AND t.id = :eventId
               AND t.status = 'COMPLETED'
             GROUP BY t.id, t.finished_at, t.accuracy, t.result
            """, nativeQuery = true)
    Optional<LearningEventProjection> findLearningEvent(
            @Param("studentId") Long studentId,
            @Param("eventId") Long eventId
    );

    @Query(value = """
            SELECT t.id AS eventId,
                   COALESCE(t.finished_at, t.started_at, t.created_at) AS occurredAt,
                   t.accuracy AS accuracy,
                   COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(t.result, '$.retryCount')) AS SIGNED), 0)
                       AS retryCount,
                   GROUP_CONCAT(
                       DISTINCT CASE
                           WHEN wal.is_correct = FALSE OR wal.is_skipped = TRUE
                           THEN wal.surface_text
                       END
                       ORDER BY wal.id SEPARATOR '|||'
                   ) AS problemSegments
              FROM tests t
              JOIN test_curriculums tc ON tc.id = t.test_curriculum_id
              LEFT JOIN word_attempt_logs wal ON wal.test_id = t.id
             WHERE tc.student_id = :studentId
               AND t.id = :eventId
               AND t.status = 'COMPLETED'
             GROUP BY t.id, t.finished_at, t.started_at, t.created_at, t.accuracy, t.result
            """, nativeQuery = true)
    Optional<LearningEventProjection> findTestLearningEvent(
            @Param("studentId") Long studentId,
            @Param("eventId") Long eventId
    );

    @Query(value = """
            SELECT s.id AS eventId,
                   s.created_at AS occurredAt,
                   AVG(wal.total_score) / 10 AS accuracy,
                   0 AS retryCount,
                   GROUP_CONCAT(
                       DISTINCT CASE
                           WHEN wal.is_correct = FALSE OR wal.is_skipped = TRUE
                           THEN wal.surface_text
                       END
                       ORDER BY wal.id SEPARATOR '|||'
                   ) AS problemSegments
              FROM stories s
              LEFT JOIN story_scenes ss ON ss.story_id = s.id
              LEFT JOIN story_lines sl ON sl.scene_id = ss.scene_id
              LEFT JOIN word_attempt_logs wal ON wal.story_line_id = sl.id
             WHERE s.student_id = :studentId
               AND s.id = :eventId
               AND s.status = 'COMPLETED'
             GROUP BY s.id, s.created_at
            """, nativeQuery = true)
    Optional<LearningEventProjection> findStoryLearningEvent(
            @Param("studentId") Long studentId,
            @Param("eventId") Long eventId
    );

    @Query(value = """
            SELECT gs.id AS eventId,
                   COALESCE(gs.ended_at, gs.created_at) AS occurredAt,
                   NULL AS accuracy,
                   0 AS retryCount,
                   CASE WHEN gs.status = 'FAILED' THEN '시선 분석 실패' ELSE NULL END
                       AS problemSegments
              FROM gaze_sessions gs
             WHERE gs.student_id = :studentId
               AND gs.id = :eventId
               AND gs.status IN ('COMPLETED', 'FAILED')
            """, nativeQuery = true)
    Optional<LearningEventProjection> findGazeLearningEvent(
            @Param("studentId") Long studentId,
            @Param("eventId") Long eventId
    );

    @Query(value = """
            SELECT tt.id AS trainingTemplateId,
                   cu.id AS curriculumUnitId,
                   cu.unit_name AS curriculumUnitName,
                   performance.averageAccuracy AS averageAccuracy
              FROM training_templates tt
              JOIN curriculum_units cu ON cu.id = tt.curriculum_unit_id
              LEFT JOIN (
                    SELECT tt2.curriculum_unit_id AS curriculumUnitId,
                           AVG(t2.accuracy) / 10 AS averageAccuracy,
                           COUNT(*) AS completedAttempts
                      FROM trainings t2
                      JOIN daily_curriculums dc2 ON dc2.id = t2.daily_curriculum_id
                      JOIN training_templates tt2 ON tt2.id = t2.training_template_id
                     WHERE dc2.student_id = :studentId
                       AND t2.status = 'COMPLETED'
                       AND t2.finished_at >= CURRENT_TIMESTAMP - INTERVAL 6 WEEK
                       AND t2.accuracy IS NOT NULL
                     GROUP BY tt2.curriculum_unit_id
              ) performance ON performance.curriculumUnitId = cu.id
             WHERE NOT EXISTS (
                    SELECT 1
                      FROM trainings completed
                      JOIN daily_curriculums completed_dc
                        ON completed_dc.id = completed.daily_curriculum_id
                     WHERE completed_dc.student_id = :studentId
                       AND completed.training_template_id = tt.id
                       AND completed.status = 'COMPLETED'
             )
             ORDER BY CASE WHEN performance.averageAccuracy IS NULL THEN 1 ELSE 0 END,
                      performance.averageAccuracy ASC,
                      performance.completedAttempts DESC,
                      cu.sequence_no ASC,
                      cu.id ASC,
                      tt.sequence_no ASC,
                      tt.id ASC
             LIMIT 1
            """, nativeQuery = true)
    Optional<TrainingRecommendationProjection> findTrainingRecommendation(
            @Param("studentId") Long studentId
    );

    @Query(value = """
            SELECT wal.training_id AS trainingId,
                   DATE(t.finished_at) AS learningDate,
                   SUM(
                       CASE
                           WHEN wal.has_audio_data = TRUE
                            AND wal.speech_start_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms > wal.speech_start_offset_ms
                            AND wal.is_correct = TRUE
                            AND COALESCE(wal.is_skipped, FALSE) = FALSE
                           THEN 1 ELSE 0
                       END
                   ) AS voiceWordCount,
                   MAX(
                       CASE
                           WHEN wal.has_audio_data = TRUE
                            AND wal.speech_start_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms > wal.speech_start_offset_ms
                           THEN wal.speech_end_offset_ms
                       END
                   ) - MIN(
                       CASE
                           WHEN wal.has_audio_data = TRUE
                            AND wal.speech_start_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms IS NOT NULL
                            AND wal.speech_end_offset_ms > wal.speech_start_offset_ms
                           THEN wal.speech_start_offset_ms
                       END
                   ) AS voiceDurationMs,
                   SUM(
                       CASE
                           WHEN wal.gaze_start_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms > wal.gaze_start_offset_ms
                            AND COALESCE(wal.is_skipped, FALSE) = FALSE
                           THEN 1 ELSE 0
                       END
                   ) AS gazeWordCount,
                   MAX(
                       CASE
                           WHEN wal.gaze_start_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms > wal.gaze_start_offset_ms
                           THEN wal.gaze_end_offset_ms
                       END
                   ) - MIN(
                       CASE
                           WHEN wal.gaze_start_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms IS NOT NULL
                            AND wal.gaze_end_offset_ms > wal.gaze_start_offset_ms
                           THEN wal.gaze_start_offset_ms
                       END
                   ) AS gazeDurationMs
              FROM word_attempt_logs wal
              JOIN trainings t ON t.id = wal.training_id
              JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
              JOIN training_templates tt ON tt.id = t.training_template_id
             WHERE dc.student_id = :studentId
               AND wal.use_location = 'TRAINING'
               AND t.status = 'COMPLETED'
               AND t.finished_at >= :fromDateTime
               AND t.finished_at < :toDateTimeExclusive
               AND JSON_UNQUOTE(JSON_EXTRACT(tt.prompt, '$.questionType')) IN (
                   'WORD_GRID_READING',
                   'SENTENCE_READING',
                   'PASSAGE_READING'
               )
             GROUP BY wal.training_id, DATE(t.finished_at)
             ORDER BY DATE(t.finished_at), wal.training_id
            """, nativeQuery = true)
    List<ReadingSpeedTrainingProjection> findReadingSpeedTrainings(
            @Param("studentId") Long studentId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive
    );

    @Modifying
    @Query(value = "DELETE FROM word_attempt_logs WHERE student_id = :studentId", nativeQuery = true)
    void deleteWordAttemptLogsByStudentId(@Param("studentId") Long studentId);

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
        BigDecimal getAverageScore();
    }

    interface TrainingHistoryProjection {
        Long getTrainingId();
        LocalDate getLearningDate();
        String getLearningType();
        String getLearningCategory();
        LocalDateTime getStartedAt();
        LocalDateTime getFinishedAt();
        BigDecimal getAchievement();
        String getResult();
    }

    interface ReadingSpeedTrainingProjection {
        Long getTrainingId();
        LocalDate getLearningDate();
        Long getVoiceWordCount();
        Long getVoiceDurationMs();
        Long getGazeWordCount();
        Long getGazeDurationMs();
    }

    interface LearningOverviewProjection {
        String getCurrentStage();
        LocalDateTime getLastLearningAt();
        Long getRecentCompletedCount();
        BigDecimal getRecentAverageAccuracy();
        Long getRecentGazeFailureCount();
    }

    interface LearningEventProjection {
        Long getEventId();
        LocalDateTime getOccurredAt();
        BigDecimal getAccuracy();
        Long getRetryCount();
        String getProblemSegments();
    }

    interface TrainingRecommendationProjection {
        Long getTrainingTemplateId();
        Long getCurriculumUnitId();
        String getCurriculumUnitName();
        BigDecimal getAverageAccuracy();
    }
}
