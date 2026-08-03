package com.iread.backend.student.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudentDeletionRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<String> findGazeDataUrls(Long studentId) {
        return jdbcTemplate.queryForList(
                """
                SELECT data_url
                  FROM gaze_sessions
                 WHERE student_id = ?
                   AND data_url IS NOT NULL
                   AND data_url <> ''
                """,
                String.class,
                studentId
        );
    }

    public StudentDeletionSummary deleteDependencies(Long studentId) {
        int authRefreshSessions = deleteDirect("auth_refresh_sessions", studentId);
        int gazeAnalysisResults = jdbcTemplate.update("""
                DELETE FROM gaze_analysis_results
                 WHERE gaze_session_id IN (
                       SELECT id FROM gaze_sessions WHERE student_id = ?
                 )
                """, studentId);
        int wordAttemptLogs = deleteDirect("word_attempt_logs", studentId);
        int trainingData = jdbcTemplate.update("""
                DELETE FROM training_datas
                 WHERE train_id IN (
                       SELECT t.id
                         FROM trainings t
                         JOIN daily_curriculums dc ON dc.id = t.daily_curriculum_id
                        WHERE dc.student_id = ?
                 )
                """, studentId);
        int testData = jdbcTemplate.update("""
                DELETE FROM test_datas
                 WHERE test_id IN (
                       SELECT t.id
                         FROM tests t
                         JOIN test_curriculums tc ON tc.id = t.test_curriculum_id
                        WHERE tc.student_id = ?
                 )
                """, studentId);
        int storyChoices = jdbcTemplate.update("""
                DELETE FROM story_choices
                 WHERE story_line_id IN (
                       SELECT sl.id
                         FROM story_lines sl
                         JOIN story_scenes ss ON ss.scene_id = sl.scene_id
                         JOIN stories s ON s.id = ss.story_id
                        WHERE s.student_id = ?
                 )
                """, studentId);
        int reports = deleteDirect("reports", studentId);
        int studentFeatureProfiles = deleteDirect("student_feature_profiles", studentId);
        int characters = deleteDirect("characters", studentId);
        int gazeSessions = deleteDirect("gaze_sessions", studentId);
        int trainings = jdbcTemplate.update("""
                DELETE FROM trainings
                 WHERE daily_curriculum_id IN (
                       SELECT id FROM daily_curriculums WHERE student_id = ?
                 )
                """, studentId);
        int tests = jdbcTemplate.update("""
                DELETE FROM tests
                 WHERE test_curriculum_id IN (
                       SELECT id FROM test_curriculums WHERE student_id = ?
                 )
                """, studentId);
        int storyLines = jdbcTemplate.update("""
                DELETE FROM story_lines
                 WHERE scene_id IN (
                       SELECT ss.scene_id
                         FROM story_scenes ss
                         JOIN stories s ON s.id = ss.story_id
                        WHERE s.student_id = ?
                 )
                """, studentId);
        int storyScenes = jdbcTemplate.update("""
                DELETE FROM story_scenes
                 WHERE story_id IN (
                       SELECT id FROM stories WHERE student_id = ?
                 )
                """, studentId);
        int dailyCurriculums = deleteDirect("daily_curriculums", studentId);
        int stories = deleteDirect("stories", studentId);
        int testCurriculums = deleteDirect("test_curriculums", studentId);

        return new StudentDeletionSummary(
                authRefreshSessions,
                gazeAnalysisResults,
                wordAttemptLogs,
                trainingData,
                testData,
                storyChoices,
                reports,
                studentFeatureProfiles,
                characters,
                gazeSessions,
                trainings,
                tests,
                storyLines,
                storyScenes,
                dailyCurriculums,
                stories,
                testCurriculums
        );
    }

    private int deleteDirect(String tableName, Long studentId) {
        return jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE student_id = ?",
                studentId
        );
    }

    public record StudentDeletionSummary(
            int authRefreshSessions,
            int gazeAnalysisResults,
            int wordAttemptLogs,
            int trainingData,
            int testData,
            int storyChoices,
            int reports,
            int studentFeatureProfiles,
            int characters,
            int gazeSessions,
            int trainings,
            int tests,
            int storyLines,
            int storyScenes,
            int dailyCurriculums,
            int stories,
            int testCurriculums
    ) {
        public int totalDeleted() {
            return authRefreshSessions
                    + gazeAnalysisResults
                    + wordAttemptLogs
                    + trainingData
                    + testData
                    + storyChoices
                    + reports
                    + studentFeatureProfiles
                    + characters
                    + gazeSessions
                    + trainings
                    + tests
                    + storyLines
                    + storyScenes
                    + dailyCurriculums
                    + stories
                    + testCurriculums;
        }
    }
}
