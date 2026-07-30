package com.iread.backend;

import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("mysql-test")
@EnabledIfEnvironmentVariable(named = "IREAD_MYSQL_TEST_ENABLED", matches = "true")
class MySqlFlywayIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private StudentTestRepository studentTestRepository;

    @Test
    void appliesAllMigrationsAndValidatesJpaMappings() {
        assertThat(applicationTableCount()).isEqualTo(25);
        assertThat(constraintCount("FOREIGN KEY")).isEqualTo(34);
        assertThat(constraintCount("UNIQUE")).isEqualTo(12);
        assertThat(constraintCount("CHECK")).isEqualTo(11);

        assertThat(tableExists("training_datas")).isTrue();
        assertThat(columnExists("training_datas", "train_id")).isTrue();
        assertThat(columnExists("training_templates", "prompt")).isTrue();
        assertThat(columnExists("training_templates", "form")).isFalse();
        assertThat(tableExists("reading_features")).isTrue();
        assertThat(tableExists("student_feature_profiles")).isTrue();
        assertThat(columnExists("student_feature_profiles", "reading_features_id")).isTrue();
        assertThat(columnExists("student_feature_profiles", "avg_pronunciation_scor")).isTrue();
        assertThat(columnExists("word_attempt_logs", "pronunciation_accuracy_score")).isTrue();
        assertThat(columnExists("word_attempt_logs", "question_no")).isTrue();
        assertThat(columnExists("word_attempt_logs", "target_index")).isTrue();
        assertThat(columnExists("word_attempt_logs", "token_index")).isTrue();
        assertThat(columnExists("word_attempt_logs", "is_final")).isTrue();
        assertThat(columnExists("word_attempt_logs", "recognized_text")).isFalse();
        assertThat(columnExists("word_attempt_logs", "has_gaze_data")).isFalse();
        assertThat(constraintExists(
                "word_attempt_logs",
                "CHK_WORD_ATTEMPT_LOGS_PRONUNCIATION_ACCURACY_SCORE",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "word_attempt_logs",
                "CHK_WORD_ATTEMPT_LOGS_TOTAL_SCORE",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "word_attempt_logs",
                "CHK_WORD_ATTEMPT_LOGS_QUESTION_NO",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "word_attempt_logs",
                "CHK_WORD_ATTEMPT_LOGS_TARGET_INDEX",
                "CHECK"
        )).isTrue();
        assertThat(constraintExists(
                "word_attempt_logs",
                "CHK_WORD_ATTEMPT_LOGS_TOKEN_INDEX",
                "CHECK"
        )).isTrue();
        assertThat(tableExists("test_datas")).isTrue();
        assertThat(tableExists("auth_refresh_sessions")).isTrue();
        assertThat(tableExists("password_reset_tokens")).isTrue();
        assertThat(columnExists("password_reset_tokens", "teacher_id")).isTrue();
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
        assertThat(tableExists("gaze_analysis_results")).isFalse();
        assertThat(constraintExists(
                "reports",
                "UQ_REPORTS_STUDENT_PERIOD",
                "UNIQUE"
        )).isTrue();

        assertThat(tableExists("training_contents")).isFalse();
        assertThat(tableExists("test_questions")).isFalse();
        assertThat(tableExists("auth_revoked_access_tokens")).isFalse();
    }

    @Test
    void wordAttemptLogSchemaEnforcesScoresPositionsAndFinalDefault() {
        long teacherId = insertAndReturnKey(
                "INSERT INTO teachers(email, password, name, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                "word-" + UUID.randomUUID().toString().substring(0, 12) + "@x.io",
                "password",
                "교사"
        );
        long studentId = insertAndReturnKey(
                "INSERT INTO students(teacher_id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                teacherId, "학생"
        );
        String wordText = "검증" + UUID.randomUUID().toString().substring(0, 8);
        long wordId = insertAndReturnKey(
                "INSERT INTO words(content, length) VALUES (?, ?)",
                wordText, wordText.length()
        );
        long curriculumUnitId = insertAndReturnKey(
                "INSERT INTO curriculum_units(unit_name, sequence_no) VALUES (?, ?)",
                "단어 수행 검증", 1
        );
        long templateId = insertAndReturnKey(
                """
                INSERT INTO training_templates(curriculum_unit_id, name, prompt, sequence_no)
                VALUES (?, ?, JSON_OBJECT('trainingType', 'WORD_READING'), 1)
                """,
                curriculumUnitId, "단어 수행 검증"
        );
        long dailyCurriculumId = insertAndReturnKey(
                """
                INSERT INTO daily_curriculums(student_id, status, created_at)
                VALUES (?, 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """,
                studentId
        );
        long trainingId = insertAndReturnKey(
                """
                INSERT INTO trainings(
                    training_template_id, daily_curriculum_id, sequence_no,
                    created_at, status
                )
                VALUES (?, ?, 1, CURRENT_TIMESTAMP, 'IN_PROGRESS')
                """,
                templateId, dailyCurriculumId
        );

        long attemptId = insertWordAttempt(
                studentId, wordId, trainingId, 1000, 1000, 1, 0, 0
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_final FROM word_attempt_logs WHERE id = ?",
                Boolean.class,
                attemptId
        )).isTrue();
        assertWordAttemptRejected(studentId, wordId, trainingId, 1001, 1000, 1, 0, 0);
        assertWordAttemptRejected(studentId, wordId, trainingId, 1000, -1, 1, 0, 0);
        assertWordAttemptRejected(studentId, wordId, trainingId, 1000, 1000, 0, 0, 0);
        assertWordAttemptRejected(studentId, wordId, trainingId, 1000, 1000, 1, -1, 0);
        assertWordAttemptRejected(studentId, wordId, trainingId, 1000, 1000, 1, 0, -1);
    }

    @Test
    void concurrentStoryChoiceWritesKeepSingleResult() throws Exception {
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
        ConcurrentResult storyChoiceResult = runConcurrently(() -> jdbcTemplate.update(
                """
                INSERT INTO story_choices(story_line_id, content, created_at)
                VALUES (?, '친구를 따라간다', CURRENT_TIMESTAMP)
                """,
                lineId
        ));

        assertThat(storyChoiceResult.successes()).isEqualTo(1);
        assertThat(storyChoiceResult.conflicts()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM story_choices WHERE story_line_id = ?",
                Integer.class,
                lineId
        )).isEqualTo(1);
    }

    @Test
    void trainingAndTestRowLocksPreventConcurrentResultLoss() throws Exception {
        long teacherId = insertAndReturnKey(
                "INSERT INTO teachers(email, password, name, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                "lock-" + UUID.randomUUID() + "@x.io", "password", "교사"
        );
        long studentId = insertAndReturnKey(
                "INSERT INTO students(teacher_id, name, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                teacherId, "학생"
        );
        long curriculumUnitId = insertAndReturnKey(
                "INSERT INTO curriculum_units(unit_name, sequence_no) VALUES (?, ?)",
                "동시성 단원", 1
        );
        long templateId = insertAndReturnKey(
                """
                INSERT INTO training_templates(curriculum_unit_id, name, prompt, sequence_no)
                VALUES (?, ?, JSON_OBJECT('trainingType', 'VOWEL_TRACE'), 1)
                """,
                curriculumUnitId, "동시성 훈련"
        );
        long dailyCurriculumId = insertAndReturnKey(
                """
                INSERT INTO daily_curriculums(student_id, status, created_at)
                VALUES (?, 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """,
                studentId
        );
        long trainingId = insertAndReturnKey(
                """
                INSERT INTO trainings(
                    training_template_id, daily_curriculum_id, sequence_no,
                    created_at, status, result
                )
                VALUES (?, ?, 1, CURRENT_TIMESTAMP, 'IN_PROGRESS', JSON_OBJECT('count', 0))
                """,
                templateId, dailyCurriculumId
        );
        long testCurriculumId =
                9_000_000_000L + Integer.toUnsignedLong(UUID.randomUUID().hashCode());
        jdbcTemplate.update(
                """
                INSERT INTO test_curriculums(id, student_id, status, created_at)
                VALUES (?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """,
                testCurriculumId, studentId
        );
        long testId = insertAndReturnKey(
                """
                INSERT INTO tests(
                    test_curriculum_id, training_template_id, status, result,
                    created_at, sequence_no
                )
                VALUES (?, ?, 'IN_PROGRESS', JSON_OBJECT('count', 0), CURRENT_TIMESTAMP, 1)
                """,
                testCurriculumId, templateId
        );

        runTwiceConcurrently(() -> {
            var training = trainingRepository.findForUpdate(trainingId, studentId).orElseThrow();
            int count = jsonCount(training.getResult());
            pauseInsideLock();
            training.recordProgressResult("{\"count\":" + (count + 1) + "}");
        });
        runTwiceConcurrently(() -> {
            var test = studentTestRepository.findByIdAndStudentIdForUpdate(testId, studentId).orElseThrow();
            int count = jsonCount(test.getResult());
            pauseInsideLock();
            test.updateResult("{\"count\":" + (count + 1) + "}");
        });

        assertThat(storedJsonCount("trainings", trainingId)).isEqualTo(2);
        assertThat(storedJsonCount("tests", testId)).isEqualTo(2);
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

    private long insertWordAttempt(
            long studentId,
            long wordId,
            long trainingId,
            int pronunciationAccuracyScore,
            int totalScore,
            int questionNo,
            int targetIndex,
            int tokenIndex
    ) {
        return insertAndReturnKey(
                """
                INSERT INTO word_attempt_logs(
                    student_id, word_id, training_id, use_location,
                    surface_text, has_audio_data, pronunciation_accuracy_score,
                    total_score, question_no, target_index, token_index
                )
                VALUES (?, ?, ?, 'TRAINING', '검증', true, ?, ?, ?, ?, ?)
                """,
                studentId, wordId, trainingId, pronunciationAccuracyScore,
                totalScore, questionNo, targetIndex, tokenIndex
        );
    }

    private void assertWordAttemptRejected(
            long studentId,
            long wordId,
            long trainingId,
            int pronunciationAccuracyScore,
            int totalScore,
            int questionNo,
            int targetIndex,
            int tokenIndex
    ) {
        assertThatThrownBy(() -> insertWordAttempt(
                studentId,
                wordId,
                trainingId,
                pronunciationAccuracyScore,
                totalScore,
                questionNo,
                targetIndex,
                tokenIndex
        )).isInstanceOf(DataAccessException.class);
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

    private void runTwiceConcurrently(Runnable mutation) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> task = () -> {
                transactionTemplate.executeWithoutResult(status -> {
                    await(barrier);
                    mutation.run();
                });
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private int storedJsonCount(String tableName, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT CAST(JSON_UNQUOTE(JSON_EXTRACT(result, '$.count')) AS SIGNED)"
                        + " FROM " + tableName + " WHERE id = ?",
                Integer.class,
                id
        );
    }

    private int jsonCount(String json) {
        int colon = json.indexOf(':');
        int end = json.indexOf('}', colon);
        return Integer.parseInt(json.substring(colon + 1, end).trim());
    }

    private void pauseInsideLock() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 검증이 중단되었습니다.", exception);
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
