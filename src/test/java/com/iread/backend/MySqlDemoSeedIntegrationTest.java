package com.iread.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "iread.training-template-seed.enabled=false")
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
    }

    private Integer count(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
    }
}
