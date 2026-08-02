package com.iread.backend;

import com.iread.backend.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ai.mock-generate=true")
@ActiveProfiles({"mysql-test", "demo"})
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlSkillChallengeReferenceDataIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StudentService studentService;

    @Test
    void alignsReferenceDataAndKeepsApprovedDemoCurriculumPolicies() {
        assertThat(jdbcTemplate.queryForList(
                """
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank
                """,
                String.class
        )).containsExactly("1", "2", "3", "4", "5");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM curriculum_units WHERE id BETWEEN 1 AND 8",
                Integer.class
        )).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_templates WHERE id BETWEEN 1 AND 34",
                Integer.class
        )).isEqualTo(34);
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
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT GROUP_CONCAT(id ORDER BY id)
                  FROM training_templates
                 WHERE JSON_UNQUOTE(JSON_EXTRACT(prompt, '$.trainingType')) IN (
                       'WORD_READING',
                       'SENTENCE_READING',
                       'SHORT_PASSAGE_READING'
                 )
                """,
                String.class
        )).isEqualTo("22,25,26");

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
                "SELECT COUNT(*) FROM trainings WHERE daily_curriculum_id = 190001",
                Integer.class
        )).isEqualTo(34);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT training_template_id) FROM trainings "
                        + "WHERE daily_curriculum_id = 190001",
                Integer.class
        )).isEqualTo(34);

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

        var readingSpeed = studentService.getReadingSpeedTrend(
                1001L,
                2001L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );
        assertThat(readingSpeed.points()).hasSize(3);
        assertThat(readingSpeed.points())
                .allSatisfy(point -> assertThat(point.voiceSpeed()).isNotNull());
    }
}
