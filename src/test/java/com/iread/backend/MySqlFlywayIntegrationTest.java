package com.iread.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mysql-test")
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlFlywayIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void appliesAllMigrationsAndValidatesJpaMappings() {
        assertThat(applicationTableCount()).isEqualTo(23);
        assertThat(constraintCount("FOREIGN KEY")).isEqualTo(31);
        assertThat(constraintCount("UNIQUE")).isEqualTo(11);
        assertThat(constraintCount("CHECK")).isEqualTo(7);

        assertThat(tableExists("training_datas")).isTrue();
        assertThat(columnExists("training_datas", "train_id")).isTrue();
        assertThat(tableExists("test_datas")).isTrue();
        assertThat(tableExists("auth_refresh_sessions")).isTrue();
        assertThat(constraintExists(
                "auth_refresh_sessions",
                "CHK_AUTH_REFRESH_SESSIONS_AUDIENCE",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "gaze_sessions",
                "CHK_GAZE_SESSIONS_CONTENT",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "story_choices",
                "UK_STORY_CHOICES_STORY_LINE",
                "UNIQUE"
        )).isTrue();
        assertThat(constraintExists(
                "gaze_analysis_results",
                "UK_GAZE_ANALYSIS_RESULTS_SESSION",
                "UNIQUE"
        )).isTrue();

        assertThat(tableExists("training_contents")).isFalse();
        assertThat(tableExists("test_questions")).isFalse();
        assertThat(tableExists("auth_revoked_access_tokens")).isFalse();
    }

    @Test
    void concurrentStoryChoiceAndGazeAnalysisWritesKeepSingleResult() throws Exception {
        long teacherId = insertAndReturnKey(
                "INSERT INTO teachers(email, password, name, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                "ci-" + UUID.randomUUID() + "@x.io", "password", "교사"
        );
        long studentId = insertAndReturnKey(
                "INSERT INTO students(teacher_id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                teacherId, "학생"
        );
        long templateId = insertAndReturnKey(
                "INSERT INTO story_templates(title, content) VALUES (?, ?)",
                "동시성 이야기", "동시 저장 검증"
        );
        long storyId = insertAndReturnKey(
                """
                INSERT INTO stories(student_id, story_template_id, created_at, status, progress)
                VALUES (?, ?, CURRENT_TIMESTAMP, 'IN_PROGRESS', 0)
                """,
                studentId, templateId
        );
        long sceneId = insertAndReturnKey(
                """
                INSERT INTO story_scenes(story_id, sequence_no, created_at)
                VALUES (?, 1, CURRENT_TIMESTAMP)
                """,
                storyId
        );
        long lineId = insertAndReturnKey(
                """
                INSERT INTO story_lines(scene_id, has_choices, content, sequence_no, created_at, read_at)
                VALUES (?, true, '어디로 갈까요?', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                sceneId
        );
        long gazeSessionId = insertAndReturnKey(
                """
                INSERT INTO gaze_sessions(
                    student_id, story_id, content_type, started_at, ended_at,
                    status, calibration_status, created_at
                )
                VALUES (?, ?, 'STORY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                        'COMPLETED', 'SUCCESS', CURRENT_TIMESTAMP)
                """,
                studentId, storyId
        );

        ConcurrentResult storyChoiceResult = runConcurrently(() -> jdbcTemplate.update(
                """
                INSERT INTO story_choices(story_line_id, content, created_at)
                VALUES (?, '친구를 따라간다', CURRENT_TIMESTAMP)
                """,
                lineId
        ));
        ConcurrentResult gazeResult = runConcurrently(() -> jdbcTemplate.update(
                """
                INSERT INTO gaze_analysis_results(
                    gaze_session_id, total_visited_duration, total_visited_count,
                    reverse_read_count, avg_visited_duration, created_at
                )
                VALUES (?, 1200, 8, 2, 150, CURRENT_TIMESTAMP)
                """,
                gazeSessionId
        ));

        assertThat(storyChoiceResult.successes()).isEqualTo(1);
        assertThat(storyChoiceResult.conflicts()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM story_choices WHERE story_line_id = ?",
                Integer.class,
                lineId
        )).isEqualTo(1);
        assertThat(gazeResult.successes()).isEqualTo(1);
        assertThat(gazeResult.conflicts()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gaze_analysis_results WHERE gaze_session_id = ?",
                Integer.class,
                gazeSessionId
        )).isEqualTo(1);
    }

    private int applicationTableCount() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }

    private int constraintCount(String constraintType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_type = ?
                """,
                Integer.class,
                constraintType
        );
        return count == null ? 0 : count;
    }

    private boolean constraintExists(String tableName, String constraintName, String constraintType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                  AND constraint_type = ?
                """,
                Integer.class,
                tableName,
                constraintName,
                constraintType
        );
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private long insertAndReturnKey(String sql, Object... arguments) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private ConcurrentResult runConcurrently(Runnable insert) throws Exception {
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> task = () -> {
                try {
                    transactionTemplate.executeWithoutResult(status -> {
                        await(barrier);
                        insert.run();
                    });
                    successes.incrementAndGet();
                } catch (DataIntegrityViolationException exception) {
                    conflicts.incrementAndGet();
                }
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            first.get();
            second.get();
            return new ConcurrentResult(successes.get(), conflicts.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception exception) {
            throw new IllegalStateException("동시성 테스트 시작점을 맞추지 못했습니다.", exception);
        }
    }

    private record ConcurrentResult(int successes, int conflicts) {
    }
}
