package com.iread.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iread-demo-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "iread.training-template-seed.enabled=false"
})
@ActiveProfiles("demo")
@Sql({
        "/db/demo/V2__demo_seed.sql",
        "/db/demo/V3__fix_demo_student_gender.sql",
        "/db/demo/V4__complete_demo_training_questions.sql",
        "/db/demo/V5__add_second_demo_student.sql"
})
class DemoSeedIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void appliesNonIdentifyingDemoSeedAndPasswordIsUsable() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );

        assertThat(passwordEncoder.matches("demo1234", passwordHash)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gender FROM students WHERE id = 2001",
                String.class
        )).isEqualTo("Girl");
        assertThat(count("students", 2001L)).isEqualTo(1);
        assertThat(count("stories", 6001L)).isEqualTo(1);
        assertThat(count("trainings", 4001L)).isEqualTo(1);
        assertThat(count("tests", 5101L)).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM students WHERE id = 2002 AND teacher_id = 1001",
                String.class
        )).isEqualTo("한결");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gender FROM students WHERE id = 2002",
                String.class
        )).isEqualTo("Boy");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.student_id = 2002
                """,
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM training_datas data
                  JOIN trainings training
                    ON training.id = data.train_id
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.student_id = 2002
                """,
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stories WHERE student_id = 2002",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM character WHERE student_id = 2002",
                Integer.class
        )).isEqualTo(1);
        assertThat(count("tests", 5102L)).isEqualTo(1);
    }

    private Integer count(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
    }
}
