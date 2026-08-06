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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email FROM teachers WHERE id = 1001",
                String.class
        )).isEqualTo("test@test.com");
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
        )).isEqualTo(66);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM gaze_analysis_results analysis
                  JOIN gaze_sessions session ON session.id = analysis.gaze_session_id
                 WHERE session.student_id IN (2001, 2002, 2103)
                   AND session.content_type = 'TRAINING'
                   AND session.status = 'COMPLETED'
                """,
                Integer.class
        )).isEqualTo(44);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM reports
                 WHERE id IN (370011, 370012, 370021, 370022, 370031, 370032)
                   AND JSON_UNQUOTE(
                         JSON_EXTRACT(snapshot_data, '$.gazeTrend.training.status')
                       ) = 'AVAILABLE'
                   AND JSON_LENGTH(
                         JSON_EXTRACT(snapshot_data, '$.gazeTrend.training.points')
                       ) = 2
                """,
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
        for (long studentId : new long[]{2001L, 2002L, 2103L}) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM test_curriculums WHERE student_id=? AND status='COMPLETED'",
                    Integer.class,
                    studentId
            )).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                      FROM tests test
                      JOIN test_curriculums curriculum
                        ON curriculum.id = test.test_curriculum_id
                     WHERE curriculum.student_id=?
                       AND test.status='COMPLETED'
                       AND JSON_LENGTH(JSON_EXTRACT(test.result, '$.questions'))=3
                    """,
                    Integer.class,
                    studentId
            )).isEqualTo(6);
            assertThat(jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                      FROM gaze_sessions session
                      JOIN tests test ON test.id = session.test_id
                      JOIN test_curriculums curriculum
                        ON curriculum.id = test.test_curriculum_id
                     WHERE curriculum.student_id=?
                       AND session.content_type='TEST'
                       AND session.status='COMPLETED'
                    """,
                    Integer.class,
                    studentId
            )).isEqualTo(6);
        }
    }

    private String teacherPassword() {
        return jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );
    }
}
