package com.iread.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mysql-test")
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlFlywayIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesAllMigrationsAndValidatesJpaMappings() {
        assertThat(applicationTableCount()).isEqualTo(25);
        assertThat(constraintCount("FOREIGN KEY")).isEqualTo(34);
        assertThat(constraintCount("UNIQUE")).isEqualTo(11);
        assertThat(constraintCount("CHECK")).isEqualTo(7);

        assertThat(tableExists("training_datas")).isTrue();
        assertThat(columnExists("training_datas", "train_id")).isTrue();
        assertThat(columnExists("training_templates", "prompt")).isTrue();
        assertThat(columnExists("training_templates", "form")).isFalse();
        assertThat(tableExists("reading_features")).isTrue();
        assertThat(tableExists("student_feature_profiles")).isTrue();
        assertThat(columnExists("student_feature_profiles", "reading_features_id")).isTrue();
        assertThat(columnExists("student_feature_profiles", "avg_pronunciation_scor")).isTrue();
        assertThat(tableExists("test_datas")).isTrue();
        assertThat(tableExists("auth_refresh_sessions")).isTrue();
        assertThat(constraintExists(
                "auth_refresh_sessions",
                "CHK_AUTH_REFRESH_SESSIONS_AUDIENCE",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "gaze_sessions",
                "CHK_GAZE_SESSIONS_CONTENT",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "story_choices",
                "UK_STORY_CHOICES_STORY_LINE",
                "UNIQUE"
        )).isTrue();
        assertThat(constraintExists(
                "gaze_analysis_results",
                "UK_GAZE_ANALYSIS_RESULTS_SESSION",
                "UNIQUE"
        )).isTrue();

        assertThat(tableExists("training_contents")).isFalse();
        assertThat(tableExists("test_questions")).isFalse();
        assertThat(tableExists("auth_revoked_access_tokens")).isFalse();
    }

    private int applicationTableCount() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }

    private int constraintCount(String constraintType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_type = ?
                """,
                Integer.class,
                constraintType
        );
        return count == null ? 0 : count;
    }

    private boolean constraintExists(String tableName, String constraintName, String constraintType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                  AND constraint_type = ?
                """,
                Integer.class,
                tableName,
                constraintName,
                constraintType
        );
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
