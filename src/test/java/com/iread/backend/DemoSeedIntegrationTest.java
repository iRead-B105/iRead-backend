package com.iread.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.iread.backend.global.config.QaDemoDatasetService;
import com.iread.backend.report.admin.dto.res.ReportSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iread-demo-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "iread.training-template-seed.enabled=false",
        "iread.teacher-demo-seed.enabled=false",
        "iread.qa-demo-dataset.enabled=false"
})
@ActiveProfiles("demo")
@Sql({
        "/db/demo/V2__demo_seed.sql",
        "/db/demo/V10__backfill_default_student_profile_images.sql"
})
class DemoSeedIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired QaDemoDatasetService qaDemoDatasetService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void appliesNonIdentifyingDemoSeedAndPasswordIsUsable() throws Exception {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );

        assertThat(passwordEncoder.matches("demo1234", passwordHash)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gender FROM students WHERE id = 2001",
                String.class
        )).isEqualTo("Girl");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT image_url FROM students WHERE id = 2001",
                String.class
        )).isEqualTo("/images/student-profile-girl.png");
        assertThat(count("students", 2001L)).isEqualTo(1);
        assertThat(count("stories", 6001L)).isZero();
        assertThat(countByColumn("story_scenes", "scene_id", 6101L)).isZero();
        assertThat(count("story_lines", 6201L)).isZero();
        assertThat(count("characters", 6301L)).isZero();
        assertThat(count("trainings", 4001L)).isEqualTo(1);
        assertThat(count("tests", 5101L)).isEqualTo(1);
        assertThat(trainingCount(2001L)).isEqualTo(10);
        assertThat(trainingDataCount(2001L)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT training.training_template_id
                  FROM trainings training
                 WHERE training.daily_curriculum_id = 3001
                 ORDER BY training.sequence_no
                 LIMIT 1
                """,
                Long.class
        )).isEqualTo(29L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT generated_data FROM training_datas WHERE train_id = 4001",
                String.class
        )).contains("IMAGE_SENTENCE_MATCH")
                .contains("imagePrompt");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM students WHERE id = 2002 AND teacher_id = 1001",
                String.class
        )).isEqualTo("한결");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gender FROM students WHERE id = 2002",
                String.class
        )).isEqualTo("Boy");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT image_url FROM students WHERE id = 2002",
                String.class
        )).isEqualTo("/images/student-profile-boy.png");
        assertThat(trainingCount(2002L)).isEqualTo(10);
        assertThat(trainingDataCount(2002L)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stories WHERE student_id = 2002",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM characters WHERE student_id = 2002",
                Integer.class
        )).isEqualTo(1);
        assertThat(count("tests", 5102L)).isEqualTo(1);

        jdbcTemplate.update("""
                INSERT INTO story_templates (id, title, content, image_url)
                VALUES (3, '노인과 바다', '데모', NULL),
                       (4, '신데렐라', '데모', NULL),
                       (5, '별주부전', '데모', NULL),
                       (6, '아기돼지 삼형제', '데모', NULL)
                """);

        qaDemoDatasetService.install();

        String qaPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE email = 'test@test.com'",
                String.class
        );
        assertThat(passwordEncoder.matches("qwer1234", qaPasswordHash)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE teacher_id = 1001 AND id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForList(
                "SELECT name FROM students WHERE teacher_id = 1001 ORDER BY id",
                String.class
        )).containsExactly("김OO", "이OO", "박OO");
        assertThat(jdbcTemplate.queryForList(
                "SELECT school FROM students WHERE teacher_id = 1001 ORDER BY id",
                String.class
        )).containsExactly("시연초등학교", "샛별초등학교", "샛별초등학교");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stories WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList(
                "SELECT progress FROM stories WHERE id IN (280001, 280002, 280003, 280004) ORDER BY id",
                Integer.class
        )).containsExactly(9, 4, 100, 20);
        assertThat(jdbcTemplate.queryForList(
                "SELECT image_url FROM story_scenes WHERE story_id IN (280001, 280002, 280003, 280004)",
                String.class
        )).hasSize(39).allMatch(imageUrl -> imageUrl.matches(
                "/uploads/images/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.jpg"
        ));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM story_lines WHERE scene_id IN (SELECT scene_id FROM story_scenes WHERE story_id IN (280001, 280002, 280003, 280004))",
                Integer.class
        )).isEqualTo(133);
        var storyLines = jdbcTemplate.queryForList("""
                SELECT line.has_choices, line.content, line.branch_prompt
                  FROM story_lines line
                  JOIN story_scenes scene ON scene.scene_id = line.scene_id
                 WHERE scene.story_id IN (280001, 280002, 280003, 280004)
                """);
        assertThat(storyLines).hasSize(133);
        int branchLineCount = 0;
        for (var storyLine : storyLines) {
            JsonNode content = readJson(storyLine.get("content"));
            boolean hasChoices = (Boolean) storyLine.get("has_choices");
            if (hasChoices) {
                branchLineCount += 1;
                assertThat(content.path("sentences").size()).isEqualTo(1);
                assertThat(content.path("sentences").get(0).asText()).endsWith("?");
                JsonNode branchPrompt = readJson(storyLine.get("branch_prompt"));
                assertThat(branchPrompt.path("options").size()).isEqualTo(3);
            } else {
                assertThat(content.path("sentences").size()).isEqualTo(3);
                assertThat(storyLine.get("branch_prompt")).isNull();
            }
        }
        assertThat(branchLineCount).isEqualTo(27);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gaze_sessions WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(13);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE student_id IN (2001, 2002, 2103)",
                Integer.class
        )).isEqualTo(6);
        for (long studentId : new long[]{2001L, 2002L, 2103L}) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM daily_curriculums WHERE student_id=? AND status='COMPLETED'",
                    Integer.class,
                    studentId
            )).isEqualTo(8);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM daily_curriculums WHERE student_id=? AND status='NOT_STARTED'",
                    Integer.class,
                    studentId
            )).isEqualTo(1);
            assertThat(trainingCount(studentId)).isEqualTo(45);
            assertThat(trainingDataCount(studentId)).isEqualTo(45);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reports WHERE student_id=?",
                    Integer.class,
                    studentId
            )).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tests WHERE test_curriculum_id IN (SELECT id FROM test_curriculums WHERE student_id=?)",
                    Integer.class,
                    studentId
            )).isEqualTo(3);
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM word_attempt_logs WHERE use_location='TEST' AND pronunciation_accuracy_score IS NOT NULL",
                Integer.class
        )).isEqualTo(81);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM word_attempt_logs WHERE use_location='TEST' AND has_gaze_data=TRUE",
                Integer.class
        )).isEqualTo(108);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM word_attempt_logs WHERE use_location='TEST' AND question_no=2 AND has_gaze_data=TRUE AND has_audio_data=FALSE AND pronunciation_accuracy_score IS NULL",
                Integer.class
        )).isEqualTo(27);

        String snapshotJson = jdbcTemplate.queryForObject(
                "SELECT snapshot_data FROM reports WHERE id=370011",
                String.class
        );
        if (objectMapper.readTree(snapshotJson).isTextual()) {
            snapshotJson = objectMapper.readTree(snapshotJson).textValue();
        }
        ReportSnapshot monthlySnapshot = objectMapper.readValue(
                snapshotJson,
                ReportSnapshot.class
        );
        assertThat(monthlySnapshot.snapshotVersion()).isEqualTo("teacher-report-v2");
        assertThat(monthlySnapshot.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(monthlySnapshot.learningDays()).isEqualTo(8);
        assertThat(monthlySnapshot.completedTrainingCount()).isEqualTo(40);
        assertThat(monthlySnapshot.automaticAnalysis().status())
                .isEqualTo(ReportSnapshot.AnalysisStatus.AVAILABLE);
        assertThat(monthlySnapshot.gazeTrend().training().status())
                .isEqualTo(ReportSnapshot.GazeSeriesStatus.NO_DATA);
        assertThat(monthlySnapshot.gazeTrend().test().points()).hasSize(3);
    }

    private String jsonText(Object value) {
        return value instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8)
                : String.valueOf(value);
    }

    private JsonNode readJson(Object value) throws Exception {
        JsonNode node = objectMapper.readTree(jsonText(value));
        return node.isTextual() ? objectMapper.readTree(node.textValue()) : node;
    }

    private Integer count(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
    }

    private Integer countByColumn(String table, String column, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id
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

    private Integer trainingDataCount(Long studentId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM training_datas data
                  JOIN trainings training
                    ON training.id = data.train_id
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.student_id = ?
                """,
                Integer.class,
                studentId
        );
    }
}
