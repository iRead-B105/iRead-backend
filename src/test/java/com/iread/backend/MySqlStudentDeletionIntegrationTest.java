package com.iread.backend;

import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ai.mock-generate=true")
@ActiveProfiles({"mysql-test", "demo"})
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
@Transactional
class MySqlStudentDeletionIntegrationTest {

    private static final long TEACHER_ID = 1001L;
    private static final long STUDENT_ID = 2001L;

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired StudentService studentService;
    @Autowired StudentRepository studentRepository;

    @Test
    void deletesTheStudentGraphWithoutRemovingSharedReferenceData() {
        jdbcTemplate.update("""
                INSERT INTO characters (student_id, story_id, image_url, name, created_at)
                SELECT ?, MIN(id), NULL, 'delete-test-character', CURRENT_TIMESTAMP
                  FROM stories
                 WHERE student_id = ?
                HAVING COUNT(*) > 0
                """, STUDENT_ID, STUDENT_ID);
        Map<String, Long> sharedCounts = sharedReferenceCounts();
        assertThat(studentCount()).isOne();
        assertThat(dependencyCounts())
                .extractingByKeys(
                        "gaze_analysis_results",
                        "training_datas",
                        "test_datas",
                        "story_choices",
                        "characters",
                        "daily_curriculums"
                )
                .allSatisfy(count -> assertThat(count).isPositive());

        studentService.deleteStudent(TEACHER_ID, STUDENT_ID);
        studentRepository.flush();

        assertThat(studentCount()).isZero();
        assertThat(dependencyCounts())
                .allSatisfy((table, count) -> assertThat(count)
                        .as("%s should be empty after deletion", table)
                        .isZero());
        assertThat(sharedReferenceCounts()).containsExactlyEntriesOf(sharedCounts);
    }

    @Test
    void deletesAStudentThatHasNoDependentRows() {
        long emptyStudentId = 990001L;
        jdbcTemplate.update("""
                INSERT INTO students (id, teacher_id, name, created_at)
                VALUES (?, ?, 'empty', CURRENT_TIMESTAMP)
                """, emptyStudentId, TEACHER_ID);

        studentService.deleteStudent(TEACHER_ID, emptyStudentId);
        studentRepository.flush();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE id = ?",
                Long.class,
                emptyStudentId
        )).isZero();
    }

    private Map<String, Long> dependencyCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("gaze_analysis_results", count("""
                SELECT COUNT(*)
                  FROM gaze_analysis_results gar
                  JOIN gaze_sessions gs ON gs.id = gar.gaze_session_id
                 WHERE gs.student_id = ?
                """));
        counts.put("training_datas", count("""
                SELECT COUNT(*)
                  FROM training_datas td
                  JOIN trainings t ON t.id = td.train_id
                  JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                 WHERE dc.student_id = ?
                """));
        counts.put("test_datas", count("""
                SELECT COUNT(*)
                  FROM test_datas td
                  JOIN tests t ON t.id = td.test_id
                  JOIN test_curriculums tc ON tc.id = t.test_curriculum_id
                 WHERE tc.student_id = ?
                """));
        counts.put("story_choices", count("""
                SELECT COUNT(*)
                  FROM story_choices sc
                  JOIN story_lines sl ON sl.id = sc.story_line_id
                  JOIN story_scenes ss ON ss.scene_id = sl.scene_id
                  JOIN stories s ON s.id = ss.story_id
                 WHERE s.student_id = ?
                """));
        counts.put("story_lines", count("""
                SELECT COUNT(*)
                  FROM story_lines sl
                  JOIN story_scenes ss ON ss.scene_id = sl.scene_id
                  JOIN stories s ON s.id = ss.story_id
                 WHERE s.student_id = ?
                """));
        counts.put("story_scenes", count("""
                SELECT COUNT(*)
                  FROM story_scenes ss
                  JOIN stories s ON s.id = ss.story_id
                 WHERE s.student_id = ?
                """));
        counts.put("tests", count("""
                SELECT COUNT(*)
                  FROM tests t
                  JOIN test_curriculums tc ON tc.id = t.test_curriculum_id
                 WHERE tc.student_id = ?
                """));
        counts.put("trainings", count("""
                SELECT COUNT(*)
                  FROM trainings t
                  JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                 WHERE dc.student_id = ?
                """));
        counts.put("word_attempt_logs", directCount("word_attempt_logs"));
        counts.put("gaze_sessions", directCount("gaze_sessions"));
        counts.put("characters", directCount("characters"));
        counts.put("stories", directCount("stories"));
        counts.put("daily_curriculums", directCount("daily_curriculums"));
        counts.put("test_curriculums", directCount("test_curriculums"));
        counts.put("reports", directCount("reports"));
        counts.put("student_feature_profiles", directCount("student_feature_profiles"));
        counts.put("auth_refresh_sessions", directCount("auth_refresh_sessions"));
        return counts;
    }

    private Map<String, Long> sharedReferenceCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : new String[]{
                "teachers",
                "training_templates",
                "story_templates",
                "reading_features",
                "words",
                "curriculum_units"
        }) {
            counts.put(table, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table,
                    Long.class
            ));
        }
        return counts;
    }

    private long studentCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE id = ?",
                Long.class,
                STUDENT_ID
        );
    }

    private long directCount(String table) {
        return count("SELECT COUNT(*) FROM " + table + " WHERE student_id = ?");
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class, STUDENT_ID);
    }
}
