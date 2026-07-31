package com.iread.backend;

import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ai.mock-generate=true")
@ActiveProfiles({"mysql-test", "demo"})
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlDemoSeedIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private CharacterRepository characterRepository;

    @Test
    void appliesDemoSeedToMySqlAndKeepsDemoLoginUsable() {
        assertThat(jdbcTemplate.queryForList(
                """
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank
                """,
                String.class
        )).containsExactly("1", "2");

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );

        assertThat(passwordEncoder.matches("demo1234", passwordHash)).isTrue();
        assertThat(count("students", 2001L)).isEqualTo(1);
        assertThat(count("stories", 6001L)).isZero();
        assertThat(countByColumn("story_scenes", "scene_id", 6101L)).isZero();
        assertThat(count("story_lines", 6201L)).isZero();
        assertThat(count("characters", 6301L)).isZero();
        assertThat(count("trainings", 4001L)).isEqualTo(1);
        assertThat(count("tests", 5101L)).isEqualTo(1);
        assertThat(count("students", 2101L)).isEqualTo(1);
        assertThat(count("daily_curriculums", 3203L)).isEqualTo(1);
        assertThat(count("trainings", 4203L)).isEqualTo(1);
        assertThat(count("tests", 5503L)).isEqualTo(1);
        assertThat(count("gaze_analysis_results", 7302L)).isEqualTo(1);
        assertThat(count("reports", 9101L)).isEqualTo(1);
        assertThat(tableCount("training_templates")).isEqualTo(34);
        assertThat(demoStudentCount()).isEqualTo(13);
        assertThat(trainingCount(2001L)).isGreaterThanOrEqualTo(50);
        assertThat(trainingCount(2002L)).isGreaterThanOrEqualTo(50);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE teacher_id = 1001",
                Integer.class
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT GROUP_CONCAT(id ORDER BY id)
                  FROM students
                 WHERE teacher_id = 1001
                """,
                String.class
        )).isEqualTo("2001,2002,2103");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings
                 WHERE daily_curriculum_id BETWEEN 180001 AND 180003
                """,
                Integer.class
        )).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN training_datas data ON data.train_id = training.id
                 WHERE training.daily_curriculum_id BETWEEN 180001 AND 180003
                """,
                Integer.class
        )).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN training_templates template
                    ON template.id = training.training_template_id
                 WHERE training.daily_curriculum_id BETWEEN 180001 AND 180003
                   AND JSON_CONTAINS(
                         JSON_EXTRACT(template.prompt, '$.requiredInputs'),
                         JSON_QUOTE('VOICE')
                       )
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings
                 WHERE daily_curriculum_id = 190001
                """,
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM word_attempt_logs attempt
                  JOIN trainings training ON training.id = attempt.training_id
                 WHERE training.daily_curriculum_id IN (190001, 180002, 180003)
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM gaze_sessions session
                  JOIN trainings training ON training.id = session.training_id
                 WHERE training.daily_curriculum_id IN (190001, 180002, 180003)
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT training_template_id)
                  FROM trainings
                 WHERE daily_curriculum_id = 190001
                """,
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM training_datas data
                  JOIN trainings training ON training.id = data.train_id
                 WHERE training.daily_curriculum_id = 190001
                   AND JSON_LENGTH(
                         JSON_EXTRACT(data.generated_data, '$.questions')
                       ) = 1
                """,
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings
                 WHERE daily_curriculum_id = 190001
                   AND (
                         (sequence_no = 1 AND status = 'NOT_STARTED')
                         OR (sequence_no > 1 AND status = 'NOT_READY')
                       )
                """,
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT training_template_id FROM trainings WHERE id = 4001",
                Long.class
        )).isEqualTo(29L);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT JSON_UNQUOTE(JSON_EXTRACT(generated_data, '$.questions[0].type'))
                  FROM training_datas
                 WHERE train_id = 130109
                """,
                String.class
        )).isEqualTo("SENTENCE_READING");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT JSON_UNQUOTE(JSON_EXTRACT(generated_data, '$.questions[0].answer.expectedText'))
                  FROM training_datas
                 WHERE train_id = 130109
                """,
                String.class
        )).isEqualTo("국물");
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
                 WHERE (id IN (2001, 2002) OR id BETWEEN 2101 AND 2111)
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
                 WHERE curriculum.student_id IN (2001, 2002)
                    OR curriculum.student_id BETWEEN 2101 AND 2111
                """,
                java.math.BigDecimal.class
        )).isLessThanOrEqualTo(new java.math.BigDecimal("1000"));
    }

    @Test
    @Transactional
    void replacesNotStartedCurriculumWithoutSequenceConflict() {
        Long curriculumId = jdbcTemplate.queryForObject(
                """
                SELECT id
                  FROM daily_curriculums
                 WHERE student_id = 2002
                   AND status = 'NOT_STARTED'
                 ORDER BY created_at DESC, id DESC
                 LIMIT 1
                """,
                Long.class
        );
        java.util.List<Long> templateIds = jdbcTemplate.queryForList(
                """
                SELECT id
                  FROM training_templates
                 ORDER BY id
                 LIMIT 5
                """,
                Long.class
        );

        trainingService.updateDailyCurriculum(
                1001L,
                2002L,
                curriculumId,
                new UpdateCurriculumRequest(templateIds)
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trainings WHERE daily_curriculum_id = ?",
                Integer.class,
                curriculumId
        )).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT sequence_no)
                  FROM trainings
                 WHERE daily_curriculum_id = ?
                """,
                Integer.class,
                curriculumId
        )).isEqualTo(5);
    }

    @Test
    void loadsStoryFriendsFromCharactersTable() {
        assertThat(characterRepository.findAllByStudentIdOrderByCreatedAtDesc(2001L))
                .isNotNull();
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

    private Integer tableCount(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class
        );
    }

    private Integer demoStudentCount() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM students
                 WHERE id IN (2001, 2002)
                    OR id BETWEEN 2101 AND 2111
                """,
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
