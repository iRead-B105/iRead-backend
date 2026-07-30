package com.iread.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"mysql-test", "demo"})
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlDemoSeedIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void appliesDemoSeedToMySqlAndKeepsDemoLoginUsable() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );

        assertThat(passwordEncoder.matches("demo1234", passwordHash)).isTrue();
        assertThat(count("students", 2001L)).isEqualTo(1);
        assertThat(count("stories", 6001L)).isEqualTo(1);
        assertThat(count("trainings", 4001L)).isEqualTo(1);
        assertThat(count("tests", 5101L)).isEqualTo(1);
        assertThat(count("students", 2101L)).isEqualTo(1);
        assertThat(count("daily_curriculums", 3203L)).isEqualTo(1);
        assertThat(count("trainings", 4203L)).isEqualTo(1);
        assertThat(count("tests", 5503L)).isEqualTo(1);
        assertThat(count("gaze_analysis_results", 7302L)).isEqualTo(1);
        assertThat(count("reports", 9101L)).isEqualTo(1);
        assertThat(tableCount("students")).isEqualTo(13);
        assertThat(tableCount("training_templates")).isEqualTo(34);
        assertThat(trainingCount(2001L)).isGreaterThanOrEqualTo(50);
        assertThat(trainingCount(2002L)).isGreaterThanOrEqualTo(50);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT training_template_id FROM trainings WHERE id = 4001",
                Long.class
        )).isEqualTo(29L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trainings WHERE id = 230101 AND accuracy > 100",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT student_id
                    FROM daily_curriculums
                    WHERE id BETWEEN 120000 AND 120999
                    GROUP BY student_id
                    HAVING COUNT(*) >= 3
                ) persona_students
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT student_id
                    FROM reports
                    WHERE id BETWEEN 170000 AND 170999
                    GROUP BY student_id
                    HAVING COUNT(*) >= 2
                ) persona_students
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE teacher_id = 1001
                  AND teacher_memo IS NOT NULL
                  AND guardian IS NOT NULL
                  AND guardian_contact IS NOT NULL
                  AND guardian_email IS NOT NULL
                  AND address IS NOT NULL
                  AND image_url IS NOT NULL
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT curriculum.student_id
                    FROM trainings training
                    JOIN daily_curriculums curriculum
                      ON curriculum.id = training.daily_curriculum_id
                    WHERE training.status = 'COMPLETED'
                    GROUP BY curriculum.student_id
                    HAVING COUNT(DISTINCT training.training_template_id) = 34
                ) catalog_coverage
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT student_id
                    FROM word_attempt_logs
                    WHERE id BETWEEN 300000 AND 399999
                    GROUP BY student_id
                    HAVING COUNT(DISTINCT DATE(created_at)) = 3
                ) trend_coverage
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM tests
                WHERE id BETWEEN 141000 AND 141999
                  AND JSON_LENGTH(JSON_EXTRACT(result, '$.areaScores')) = 3
                  AND JSON_LENGTH(JSON_EXTRACT(result, '$.strengthAreas')) >= 1
                  AND JSON_LENGTH(JSON_EXTRACT(result, '$.improvementAreas')) >= 1
                """,
                Integer.class
        )).isEqualTo(39);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM reports
                WHERE id BETWEEN 170000 AND 170999
                  AND JSON_UNQUOTE(
                        JSON_EXTRACT(snapshot_data, '$.gazeTrend.training.status')
                      ) = 'AVAILABLE'
                  AND JSON_LENGTH(
                        JSON_EXTRACT(snapshot_data, '$.gazeTrend.training.points')
                      ) >= 2
                """,
                Integer.class
        )).isEqualTo(26);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT curriculum.student_id)
                FROM gaze_analysis_results analysis
                JOIN gaze_sessions session ON session.id = analysis.gaze_session_id
                JOIN trainings training ON training.id = session.training_id
                JOIN daily_curriculums curriculum
                  ON curriculum.id = training.daily_curriculum_id
                WHERE session.content_type = 'TRAINING'
                  AND session.status = 'COMPLETED'
                  AND training.finished_at >= '2026-07-27 00:00:00'
                """,
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT MAX(training.accuracy)
                FROM trainings training
                JOIN daily_curriculums curriculum
                  ON curriculum.id = training.daily_curriculum_id
                JOIN students student ON student.id = curriculum.student_id
                WHERE student.teacher_id = 1001
                """,
                java.math.BigDecimal.class
        )).isLessThanOrEqualTo(new java.math.BigDecimal("1000"));
    }

    private Integer count(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
    }

    private Integer tableCount(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class
        );
    }

    private Integer trainingCount(Long studentId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.student_id = ?
                """,
                Integer.class,
                studentId
        );
    }
}
