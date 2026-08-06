package com.iread.backend;

import com.iread.backend.global.config.QaDemoDatasetService;
import com.iread.backend.global.config.QaDemoResetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ai.mock-generate=true",
        "app.file-storage.local.upload-dir=build/test-qa-demo/images",
        "app.gaze-storage.local.upload-dir=build/test-qa-demo/gaze"
})
@ActiveProfiles({"mysql-test", "demo"})
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlQaDemoResetIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QaDemoDatasetService datasetService;

    @Autowired
    private QaDemoResetService resetService;

    @Test
    void replacesTheDefaultDemoSeedOnlyWhenTheQaResetIsInvoked() {
        String defaultPasswordHash = teacherPassword();
        assertThat(passwordEncoder.matches("demo1234", defaultPasswordHash)).isTrue();
        assertThat(passwordEncoder.matches("qwer1234", defaultPasswordHash)).isFalse();

        resetService.reset();
        assertQaDataset();

        resetService.reset();
        assertQaDataset();
    }

    private void assertQaDataset() {
        assertThat(datasetService.isPostSeedInstalled()).isTrue();
        assertThat(passwordEncoder.matches("qwer1234", teacherPassword())).isTrue();
        assertThat(jdbcTemplate.queryForList(
                "SELECT name FROM students WHERE teacher_id = 1001 ORDER BY id",
                String.class
        )).containsExactly("김도윤", "이서연", "박지호");
        assertThat(jdbcTemplate.queryForList(
                "SELECT school FROM students WHERE teacher_id = 1001 ORDER BY id",
                String.class
        )).containsExactly("시연초등학교", "샛별초등학교", "샛별초등학교");
        assertThat(jdbcTemplate.queryForList(
                "SELECT progress FROM stories WHERE id IN (280001, 280002, 280003, 280004) ORDER BY id",
                Integer.class
        )).containsExactly(9, 4, 100, 20);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stories WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM story_lines line
                  JOIN story_scenes scene ON scene.scene_id = line.scene_id
                 WHERE scene.story_id IN (280001, 280002, 280003, 280004)
                """,
                Integer.class
        )).isEqualTo(133);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM story_scenes WHERE story_id IN (280001, 280002, 280003, 280004)",
                Integer.class
        )).isEqualTo(39);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gaze_sessions WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM daily_curriculums curriculum
                 WHERE curriculum.student_id IN (2001, 2002, 2103)
                   AND curriculum.status = 'COMPLETED'
                """,
                Integer.class
        )).isEqualTo(24);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM daily_curriculums curriculum
                 WHERE curriculum.student_id IN (2001, 2002, 2103)
                   AND curriculum.status = 'NOT_STARTED'
                """,
                Integer.class
        )).isEqualTo(3);
    }

    private String teacherPassword() {
        return jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );
    }
}
