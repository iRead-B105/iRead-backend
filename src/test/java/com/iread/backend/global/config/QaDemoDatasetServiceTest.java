package com.iread.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class QaDemoDatasetServiceTest {

    @Test
    void refusesToReplaceARegularTeacherUsingTheReservedQaId() {
        JdbcTemplate jdbcTemplate = jdbcTemplate("reserved-id");
        jdbcTemplate.update(
                "INSERT INTO teachers(id, email) VALUES (?, ?)",
                1001L,
                "regular@example.com"
        );

        assertThatIllegalStateException().isThrownBy(
                () -> new QaDemoDatasetService(jdbcTemplate).install()
        ).withMessageContaining("reserved id");
    }

    @Test
    void refusesToReplaceTheQaEmailWhenItBelongsToAnotherTeacher() {
        JdbcTemplate jdbcTemplate = jdbcTemplate("reserved-email");
        jdbcTemplate.update(
                "INSERT INTO teachers(id, email) VALUES (?, ?)",
                2000L,
                "test@test.com"
        );

        assertThatIllegalStateException().isThrownBy(
                () -> new QaDemoDatasetService(jdbcTemplate).install()
        ).withMessageContaining("email belongs to teacher 2000");
    }

    private JdbcTemplate jdbcTemplate(String databaseName) {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:qa-dataset-" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE teachers (id BIGINT PRIMARY KEY, email VARCHAR(50) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE students (id BIGINT PRIMARY KEY, teacher_id BIGINT NOT NULL)");
        return jdbcTemplate;
    }
}
