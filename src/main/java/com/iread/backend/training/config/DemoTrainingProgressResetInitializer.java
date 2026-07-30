package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Order(40)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-training-reset.enabled",
        havingValue = "true"
)
public class DemoTrainingProgressResetInitializer implements ApplicationRunner {

    static final Map<Long, Long> RESET_CURRICULUM_BY_STUDENT = Map.of(
            2001L, 190001L,
            2002L, 180002L,
            2103L, 180003L
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RESET_CURRICULUM_BY_STUDENT.forEach(this::resetIfPresent);
    }

    private void resetIfPresent(Long studentId, Long curriculumId) {
        Integer curriculumCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM daily_curriculums WHERE id = ? AND student_id = ?",
                Integer.class,
                curriculumId,
                studentId
        );
        if (curriculumCount == null || curriculumCount == 0) {
            return;
        }

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
