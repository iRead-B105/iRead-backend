package com.iread.backend.training.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DemoTrainingProgressResetServiceTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:demo-training-reset;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE daily_curriculums (
                    id BIGINT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    completed_at TIMESTAMP NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE trainings (
                    id BIGINT PRIMARY KEY,
                    daily_curriculum_id BIGINT NOT NULL,
                    sequence_no INT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
                    result JSON NULL,
                    accuracy INT NULL
                )
                """);
        jdbcTemplate.execute("CREATE TABLE word_attempt_logs (id BIGINT PRIMARY KEY, training_id BIGINT NULL)");
        jdbcTemplate.execute("CREATE TABLE gaze_sessions (id BIGINT PRIMARY KEY, training_id BIGINT NULL)");
        jdbcTemplate.execute("CREATE TABLE gaze_analysis_results (id BIGINT PRIMARY KEY, gaze_session_id BIGINT NOT NULL)");
    }

    @Test
    void restoresSeededCurriculumOnlyWhenExplicitlyCalled() {
        jdbcTemplate.update("""
                INSERT INTO daily_curriculums (id, student_id, status, completed_at)
                VALUES (190001, 2001, 'COMPLETED', CURRENT_TIMESTAMP),
                       (190099, 2001, 'IN_PROGRESS', NULL)
                """);
        jdbcTemplate.update("""
                INSERT INTO trainings
                    (id, daily_curriculum_id, sequence_no, status, started_at,
                     finished_at, result, accuracy)
                VALUES
                    (1, 190001, 1, 'COMPLETED', CURRENT_TIMESTAMP,
                     CURRENT_TIMESTAMP, '{"done":true}', 1000),
                    (2, 190001, 2, 'IN_PROGRESS', CURRENT_TIMESTAMP,
                     NULL, '{"question":1}', NULL)
                """);
        jdbcTemplate.update("INSERT INTO word_attempt_logs (id, training_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO gaze_sessions (id, training_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO gaze_analysis_results (id, gaze_session_id) VALUES (1, 1)");

        DemoTrainingProgressResetService resetService =
                new DemoTrainingProgressResetService(jdbcTemplate);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_curriculums WHERE id = 190001",
                String.class
        )).isEqualTo("COMPLETED");

        resetService.reset(2001L);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_curriculums WHERE id = 190001",
                String.class
        )).isEqualTo("NOT_STARTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at FROM daily_curriculums WHERE id = 190001",
                Object.class
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_curriculums WHERE id = 190099",
                String.class
        )).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM trainings WHERE daily_curriculum_id = 190001 ORDER BY sequence_no",
                String.class
        )).containsExactly("NOT_STARTED", "NOT_READY");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings
                 WHERE daily_curriculum_id = 190001
                   AND (started_at IS NOT NULL OR finished_at IS NOT NULL
                        OR result IS NOT NULL OR accuracy IS NOT NULL)
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM word_attempt_logs", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gaze_sessions", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gaze_analysis_results", Integer.class)).isZero();
    }
}
