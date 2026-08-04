package com.iread.backend;

import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        )).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM teachers WHERE id = 1001",
                String.class
        );

        assertThat(passwordEncoder.matches("demo1234", passwordHash)).isTrue();
        assertThat(count("students", 2001L)).isEqualTo(1);
        assertThat(count("stories", 6001L)).isZero();
        assertThat(countByColumn("story_scenes", "scene_id", 6101L)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_LENGTH(JSON_EXTRACT(branch_prompt, '$.options')) "
                        + "FROM story_lines WHERE id = 6603",
                Integer.class
        )).isEqualTo(3);

        assertThat(count("story_lines", 6201L)).isZero();
        assertThat(count("characters", 6301L)).isZero();
        assertThat(count("trainings", 4001L)).isEqualTo(1);
        assertThat(count("tests", 5101L)).isZero();
        assertThat(count("tests", 5102L)).isZero();
        assertThat(count("test_curriculums", 5001L)).isZero();
        assertThat(count("test_curriculums", 5002L)).isZero();
        assertThat(count("students", 2101L)).isEqualTo(1);
        assertThat(count("daily_curriculums", 3203L)).isEqualTo(1);
        assertThat(count("trainings", 4203L)).isEqualTo(1);
        assertThat(count("tests", 5503L)).isEqualTo(1);
        assertThat(count("gaze_analysis_results", 7302L)).isEqualTo(1);
        assertThat(count("reports", 9101L)).isEqualTo(1);
        assertThat(tableCount("training_templates")).isEqualTo(34);
        assertThat(tableCount("curriculum_units")).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT unit_name FROM curriculum_units WHERE id = 1",
                String.class
        )).isEqualTo("글자 따라 보기");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM training_templates WHERE id = 1",
                String.class
        )).isEqualTo("모음 따라 보기");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(prompt, '$.trainingType')) "
                        + "FROM training_templates WHERE id = 15",
                String.class
        )).isEqualTo("SYLLABLE_BLEND");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_CONTAINS_PATH(prompt, 'one', '$.questionType') "
                        + "FROM training_templates WHERE id = 15",
                Integer.class
        )).isZero();
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
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN training_templates template
                    ON template.id = training.training_template_id
                 WHERE training.daily_curriculum_id = 180003
                   AND training.sequence_no = 1
                   AND JSON_CONTAINS(
                         JSON_EXTRACT(template.prompt, '$.requiredInputs'),
                         JSON_QUOTE('VOICE')
                       )
                """,
                Integer.class
        )).isEqualTo(1);
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
                       ) = 5
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
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT curriculum.id
                          FROM daily_curriculums curriculum
                          JOIN trainings training
                            ON training.daily_curriculum_id = curriculum.id
                         WHERE curriculum.id IN (
                               120023, 120033, 120053, 120063, 120073,
                               120083, 120093, 120103, 120113, 120123
                         )
                         GROUP BY curriculum.id
                        HAVING COUNT(*) = 5
                           AND COUNT(DISTINCT training.sequence_no) = 5
                  ) corrected
                """,
                Integer.class
        )).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM training_datas data
                  JOIN trainings training ON training.id = data.train_id
                  JOIN training_templates template
                    ON template.id = training.training_template_id
                 WHERE training.id IN (
                       130213, 130313, 130513, 130613, 130713,
                       130813, 130913, 131013, 131113, 131213
                 )
                   AND JSON_LENGTH(JSON_EXTRACT(data.generated_data, '$.questions')) = 3
                   AND JSON_UNQUOTE(
                         JSON_EXTRACT(data.generated_data, '$.questions[0].type')
                       ) = JSON_UNQUOTE(JSON_EXTRACT(template.prompt, '$.trainingType'))
                   AND training.training_template_id = CASE training.id
                         WHEN 130213 THEN 18
                         WHEN 130313 THEN 21
                         WHEN 130513 THEN 27
                         WHEN 130613 THEN 30
                         WHEN 130713 THEN 33
                         WHEN 130813 THEN 2
                         WHEN 130913 THEN 5
                         WHEN 131013 THEN 8
                         WHEN 131113 THEN 11
                         WHEN 131213 THEN 14
                       END
                """,
                Integer.class
        )).isEqualTo(10);
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
    void keepsSkillChallengeDatabaseRelationshipsConsistent() {
        var orphanCounts = jdbcTemplate.queryForList(
                """
                SELECT relation_name, orphan_count
                  FROM (
                        SELECT 'training_templates.curriculum_unit_id' AS relation_name,
                               COUNT(*) AS orphan_count
                          FROM training_templates child
                          LEFT JOIN curriculum_units parent
                            ON parent.id = child.curriculum_unit_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'test_curriculums.student_id', COUNT(*)
                          FROM test_curriculums child
                          LEFT JOIN students parent ON parent.id = child.student_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'tests.test_curriculum_id', COUNT(*)
                          FROM tests child
                          LEFT JOIN test_curriculums parent
                            ON parent.id = child.test_curriculum_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'tests.training_template_id', COUNT(*)
                          FROM tests child
                          LEFT JOIN training_templates parent
                            ON parent.id = child.training_template_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'test_datas.test_id', COUNT(*)
                          FROM test_datas child
                          LEFT JOIN tests parent ON parent.id = child.test_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'daily_curriculums.student_id', COUNT(*)
                          FROM daily_curriculums child
                          LEFT JOIN students parent ON parent.id = child.student_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'daily_curriculums.source_test_curriculum_id', COUNT(*)
                          FROM daily_curriculums child
                          LEFT JOIN test_curriculums parent
                            ON parent.id = child.source_test_curriculum_id
                         WHERE child.source_test_curriculum_id IS NOT NULL
                           AND parent.id IS NULL
                        UNION ALL
                        SELECT 'daily_curriculums.reviewed_by_teacher_id', COUNT(*)
                          FROM daily_curriculums child
                          LEFT JOIN teachers parent
                            ON parent.id = child.reviewed_by_teacher_id
                         WHERE child.reviewed_by_teacher_id IS NOT NULL
                           AND parent.id IS NULL
                        UNION ALL
                        SELECT 'trainings.daily_curriculum_id', COUNT(*)
                          FROM trainings child
                          LEFT JOIN daily_curriculums parent
                            ON parent.id = child.daily_curriculum_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'trainings.training_template_id', COUNT(*)
                          FROM trainings child
                          LEFT JOIN training_templates parent
                            ON parent.id = child.training_template_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'training_datas.train_id', COUNT(*)
                          FROM training_datas child
                          LEFT JOIN trainings parent ON parent.id = child.train_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'student_feature_profiles.student_id', COUNT(*)
                          FROM student_feature_profiles child
                          LEFT JOIN students parent ON parent.id = child.student_id
                         WHERE parent.id IS NULL
                        UNION ALL
                        SELECT 'student_feature_profiles.reading_features_id', COUNT(*)
                          FROM student_feature_profiles child
                          LEFT JOIN reading_features parent
                            ON parent.id = child.reading_features_id
                         WHERE parent.id IS NULL
                  ) orphan_audit
                 ORDER BY relation_name
                """
        );
        assertThat(orphanCounts).allSatisfy(row -> assertThat(
                ((Number) row.get("orphan_count")).intValue()
        ).as(String.valueOf(row.get("relation_name"))).isZero());

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT test_curriculum_id, sequence_no
                          FROM tests
                         GROUP BY test_curriculum_id, sequence_no
                        HAVING COUNT(*) > 1
                  ) duplicates
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT daily_curriculum_id, sequence_no
                          FROM trainings
                         GROUP BY daily_curriculum_id, sequence_no
                        HAVING COUNT(*) > 1
                  ) duplicates
                """,
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT source_test_curriculum_id
                          FROM daily_curriculums
                         WHERE source_test_curriculum_id IS NOT NULL
                         GROUP BY source_test_curriculum_id
                        HAVING COUNT(*) > 1
                  ) duplicates
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT curriculum.id
                          FROM test_curriculums curriculum
                          JOIN tests test ON test.test_curriculum_id = curriculum.id
                          JOIN test_datas data ON data.test_id = test.id
                         WHERE curriculum.id BETWEEN 140001 AND 140013
                         GROUP BY curriculum.id
                        HAVING COUNT(DISTINCT test.id) = 3
                           AND COUNT(DISTINCT test.sequence_no) = 3
                           AND SUM(JSON_LENGTH(
                                 JSON_EXTRACT(data.generated_data, '$.questions')
                               )) = 9
                           AND SUM(JSON_LENGTH(
                                 JSON_EXTRACT(test.result, '$.questions')
                               )) = 9
                  ) completed_challenges
                """,
                Integer.class
        )).isEqualTo(13);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT curriculum.id
                          FROM daily_curriculums curriculum
                          JOIN trainings training
                            ON training.daily_curriculum_id = curriculum.id
                          JOIN training_datas data ON data.train_id = training.id
                         WHERE curriculum.id IN (
                               120023, 120033, 120053, 120063, 120073,
                               120083, 120093, 120103, 120113, 120123
                         )
                           AND curriculum.status = 'NOT_STARTED'
                         GROUP BY curriculum.id
                        HAVING COUNT(DISTINCT training.id) = 5
                           AND COUNT(DISTINCT training.sequence_no) = 5
                           AND COUNT(DISTINCT training.training_template_id) = 5
                           AND COUNT(DISTINCT data.id) = 5
                  ) corrected_curriculums
                """,
                Integer.class
        )).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN training_datas data ON data.train_id = training.id
                 WHERE training.daily_curriculum_id = 190001
                   AND (
                         (training.sequence_no = 1
                          AND training.status = 'NOT_STARTED')
                         OR (training.sequence_no > 1
                             AND training.status = 'NOT_READY')
                       )
                """,
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM (
                        SELECT curriculum.id
                          FROM daily_curriculums curriculum
                          JOIN trainings training
                            ON training.daily_curriculum_id = curriculum.id
                         WHERE curriculum.status = 'COMPLETED'
                         GROUP BY curriculum.id
                        HAVING COUNT(*) = 4
                  ) preserved_curriculums
                """,
                Integer.class
        )).isEqualTo(30);
    }

    @Test
    @Transactional
    void enforcesOneFiveTrainingRecommendationPerSourceTest() {
        jdbcTemplate.update(
                """
                INSERT INTO students(id, teacher_id, name, created_at)
                VALUES (990001, 1001, '관계검증1', CURRENT_TIMESTAMP),
                       (990002, 1001, '관계검증2', CURRENT_TIMESTAMP)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO test_curriculums(
                    id, student_id, status, created_at, completed_at,
                    recommendation_status, recommendation_retry_count
                )
                VALUES (
                    990001, 990001, 'COMPLETED', CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP, 'COMPLETED', 0
                )
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO daily_curriculums(
                    id, student_id, status, created_at,
                    source_test_curriculum_id, review_status
                )
                VALUES (
                    990001, 990001, 'NOT_STARTED', CURRENT_TIMESTAMP,
                    990001, 'GENERATION_PENDING'
                )
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO trainings(
                    id, training_template_id, daily_curriculum_id,
                    sequence_no, created_at, status
                )
                VALUES
                    (991001, 1, 990001, 1, CURRENT_TIMESTAMP, 'NOT_READY'),
                    (991002, 2, 990001, 2, CURRENT_TIMESTAMP, 'NOT_READY'),
                    (991003, 3, 990001, 3, CURRENT_TIMESTAMP, 'NOT_READY'),
                    (991004, 4, 990001, 4, CURRENT_TIMESTAMP, 'NOT_READY'),
                    (991005, 5, 990001, 5, CURRENT_TIMESTAMP, 'NOT_READY')
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM trainings training
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.source_test_curriculum_id = 990001
                """,
                Integer.class
        )).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT training.sequence_no)
                  FROM trainings training
                  JOIN daily_curriculums curriculum
                    ON curriculum.id = training.daily_curriculum_id
                 WHERE curriculum.source_test_curriculum_id = 990001
                """,
                Integer.class
        )).isEqualTo(5);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO daily_curriculums(
                    id, student_id, status, created_at,
                    source_test_curriculum_id, review_status
                )
                VALUES (
                    990002, 990002, 'NOT_STARTED', CURRENT_TIMESTAMP,
                    990001, 'GENERATION_PENDING'
                )
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
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
