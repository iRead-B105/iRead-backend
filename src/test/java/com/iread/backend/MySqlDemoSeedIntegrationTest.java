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
        assertThat(tableCount("students")).isEqualTo(12);
        assertThat(tableCount("training_templates")).isEqualTo(34);
        assertThat(count("reports", 170121L)).isEqualTo(1);
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
        )).isEqualTo(12);
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
        )).isEqualTo(12);
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
}
