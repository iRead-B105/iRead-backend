package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoTrainingProgressResetService {

    static final Map<Long, Long> RESET_CURRICULUM_BY_STUDENT = Map.of(
            2001L, 190001L,
            2002L, 180002L,
            2103L, 180003L
    );

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Long reset(Long studentId) {
        Long curriculumId = RESET_CURRICULUM_BY_STUDENT.get(studentId);
        if (curriculumId == null || !exists(studentId, curriculumId)) {
            throw new IllegalArgumentException("초기화할 수 있는 데모 학습자가 아닙니다: " + studentId);
        }
        reset(studentId, curriculumId);
        return curriculumId;
    }

    @Transactional
    public boolean resetIfPresent(Long studentId) {
        Long curriculumId = RESET_CURRICULUM_BY_STUDENT.get(studentId);
        if (curriculumId == null || !exists(studentId, curriculumId)) {
            return false;
        }
        reset(studentId, curriculumId);
        return true;
    }

    private boolean exists(Long studentId, Long curriculumId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM daily_curriculums WHERE id = ? AND student_id = ?",
                Integer.class,
                curriculumId,
                studentId
        );
        return count != null && count > 0;
    }

    private void reset(Long studentId, Long curriculumId) {
        jdbcTemplate.update(
                """
                UPDATE daily_curriculums
                   SET status = 'COMPLETED',
                       completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP)
                 WHERE student_id = ?
                   AND id <> ?
                   AND status IN ('NOT_STARTED', 'IN_PROGRESS')
                """,
                studentId,
                curriculumId
        );
        jdbcTemplate.update(
                """
                DELETE FROM gaze_analysis_results
                 WHERE gaze_session_id IN (
                       SELECT id
                         FROM gaze_sessions
                        WHERE training_id IN (
                              SELECT id
                                FROM trainings
                               WHERE daily_curriculum_id = ?
                        )
                 )
                """,
                curriculumId
        );
        jdbcTemplate.update(
                """
                DELETE FROM gaze_sessions
                 WHERE training_id IN (
                       SELECT id
                         FROM trainings
                        WHERE daily_curriculum_id = ?
                 )
                """,
                curriculumId
        );
        jdbcTemplate.update(
                """
                DELETE FROM word_attempt_logs
                 WHERE training_id IN (
                       SELECT id
                         FROM trainings
                        WHERE daily_curriculum_id = ?
                 )
                """,
                curriculumId
        );
        jdbcTemplate.update(
                """
                UPDATE trainings
                   SET status = CASE
                           WHEN sequence_no = 1 THEN 'NOT_STARTED'
                           ELSE 'NOT_READY'
                       END,
                       started_at = NULL,
                       finished_at = NULL,
                       result = NULL,
                       accuracy = NULL
                 WHERE daily_curriculum_id = ?
                """,
                curriculumId
        );
        jdbcTemplate.update(
                """
                UPDATE daily_curriculums
                   SET status = 'NOT_STARTED',
                       completed_at = NULL
                 WHERE id = ?
                """,
                curriculumId
        );
    }
}
