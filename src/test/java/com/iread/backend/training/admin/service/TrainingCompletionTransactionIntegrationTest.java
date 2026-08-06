package com.iread.backend.training.admin.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.test.app.dto.req.TestCompleteRequest;
import com.iread.backend.test.app.service.AppTestService;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.input.TrainingInputRequirementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class TrainingCompletionTransactionIntegrationTest {

    private static final long TEACHER_ID = 91_001L;
    private static final long STUDENT_ID = 91_002L;
    private static final long UNIT_ID = 91_003L;
    private static final long TEMPLATE_ID = 91_004L;
    private static final long CURRICULUM_ID = 91_005L;
    private static final long TRAINING_ID = 91_006L;
    private static final long TRAINING_DATA_ID = 91_007L;
    private static final long TEST_CURRICULUM_ID = 91_008L;
    private static final long TEST_ID = 91_009L;
    private static final long TEST_DATA_ID = 91_010L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppTestService appTestService;

    @MockitoBean
    private AiClient aiClient;

    @MockitoBean
    private StudentFeatureProfileService studentFeatureProfileService;

    @MockitoBean
    private RealtimeEventPublisher realtimeEventPublisher;

    @MockitoBean
    private TrainingInputRequirementService trainingInputRequirementService;

    @MockitoBean
    private TestDataRepository testDataRepository;

    @MockitoBean(name = "trainingCompletionTaskExecutor")
    private TaskExecutor trainingCompletionTaskExecutor;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(trainingCompletionTaskExecutor).execute(any(Runnable.class));

        jdbcTemplate.update("""
                INSERT INTO teachers (id, email, password, name, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, TEACHER_ID, "transaction-test@iread.local", "encoded", "교수자");
        jdbcTemplate.update("""
                INSERT INTO students (id, teacher_id, name, created_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, STUDENT_ID, TEACHER_ID, "아동");
        jdbcTemplate.update("""
                INSERT INTO curriculum_units (id, unit_name, sequence_no)
                VALUES (?, ?, ?)
                """, UNIT_ID, "트랜잭션 검증", 1);
        jdbcTemplate.update("""
                INSERT INTO training_templates (
                    id, curriculum_unit_id, name, prompt, sequence_no
                ) VALUES (?, ?, ?, ?, ?)
                """, TEMPLATE_ID, UNIT_ID, "트랜잭션 훈련", "{}", 1);
        jdbcTemplate.update("""
                INSERT INTO daily_curriculums (
                    id, student_id, status, created_at
                ) VALUES (?, ?, 'NOT_STARTED', CURRENT_TIMESTAMP)
                """, CURRICULUM_ID, STUDENT_ID);
        jdbcTemplate.update("""
                INSERT INTO trainings (
                    id, training_template_id, daily_curriculum_id, sequence_no,
                    created_at, status
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'NOT_STARTED')
                """, TRAINING_ID, TEMPLATE_ID, CURRICULUM_ID, 1);
        jdbcTemplate.update("""
                INSERT INTO training_datas (
                    id, train_id, generated_data, created_at
                ) VALUES (?, ?, ? FORMAT JSON, CURRENT_TIMESTAMP)
                """, TRAINING_DATA_ID, TRAINING_ID, """
                {"questions":[{"questionNo":1,"requiredInputs":[]}]}
                """);
        jdbcTemplate.update("""
                INSERT INTO test_curriculums (
                    id, student_id, status, created_at
                ) VALUES (?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """, TEST_CURRICULUM_ID, STUDENT_ID);
        jdbcTemplate.update("""
                INSERT INTO tests (
                    id, test_curriculum_id, training_template_id, status,
                    result, created_at, started_at, sequence_no
                ) VALUES (
                    ?, ?, ?, 'IN_PROGRESS', ? FORMAT JSON,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1
                )
                """, TEST_ID, TEST_CURRICULUM_ID, TEMPLATE_ID, """
                {"submissions":[{"submissionId":"00000000-0000-0000-0000-000000000001","questionNo":1,"totalScore":1000}]}
                """);
        jdbcTemplate.update("""
                INSERT INTO test_datas (
                    id, test_id, generated_data, created_at
                ) VALUES (?, ?, ? FORMAT JSON, CURRENT_TIMESTAMP)
                """, TEST_DATA_ID, TEST_ID, """
                {"questions":[{"questionNo":1,"type":"CONSONANT_SOUND_CHOICE"}]}
                """);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM test_datas WHERE id = ?", TEST_DATA_ID);
        jdbcTemplate.update("DELETE FROM tests WHERE id = ?", TEST_ID);
        jdbcTemplate.update(
                "DELETE FROM test_curriculums WHERE id = ?",
                TEST_CURRICULUM_ID
        );
        jdbcTemplate.update("DELETE FROM training_datas WHERE id = ?", TRAINING_DATA_ID);
        jdbcTemplate.update("DELETE FROM trainings WHERE id = ?", TRAINING_ID);
        jdbcTemplate.update("DELETE FROM daily_curriculums WHERE id = ?", CURRICULUM_ID);
        jdbcTemplate.update("DELETE FROM training_templates WHERE id = ?", TEMPLATE_ID);
        jdbcTemplate.update("DELETE FROM curriculum_units WHERE id = ?", UNIT_ID);
        jdbcTemplate.update("DELETE FROM students WHERE id = ?", STUDENT_ID);
        jdbcTemplate.update("DELETE FROM teachers WHERE id = ?", TEACHER_ID);
    }

    @Test
    void aiEvaluationFailureKeepsTrainingRetryableAndRetrySucceeds() {
        // 1차 완료 시도: AI 평가가 죽어 있으면 훈련은 완료되지 않고
        // 같은 완료 요청을 다시 보낼 수 있는 상태로 남아야 한다. (자체 QA B-3)
        when(aiClient.evaluateTraining(any()))
                .thenThrow(new com.iread.backend.ai.exception.AiClientException(
                        "AI 서버와 훈련 평가 통신 중 실패했습니다."
                ))
                .thenReturn(new EvaluateTrainingResponse(
                        "training-evaluation-" + TRAINING_ID,
                        1,
                        new BigDecimal("77.50")
                ));

        assertThatThrownBy(() -> trainingService.completeTraining(
                TEACHER_ID,
                STUDENT_ID,
                TRAINING_ID,
                objectMapper.createObjectNode(),
                LocalDateTime.of(2026, 7, 31, 11, 0)
        )).isInstanceOf(com.iread.backend.ai.exception.AiClientException.class);

        assertThat(trainingValue("status", String.class)).isNotEqualTo("COMPLETED");
        assertThat(trainingValue("finished_at", LocalDateTime.class)).isNull();

        // 2차: AI 복구 후 재시도(아이 화면 '다시 시도할래요' 버튼과 같은 호출) → 성공
        BigDecimal accuracy = trainingService.completeTraining(
                TEACHER_ID,
                STUDENT_ID,
                TRAINING_ID,
                objectMapper.createObjectNode(),
                LocalDateTime.of(2026, 7, 31, 11, 5)
        );

        assertThat(accuracy).isEqualByComparingTo("77.50");
        assertThat(trainingValue("status", String.class)).isEqualTo("COMPLETED");
        assertThat(trainingValue("finished_at", LocalDateTime.class)).isNotNull();
    }

    @Test
    void followUpFailureDoesNotRollbackCompletedTraining() {
        when(aiClient.evaluateTraining(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isFalse();
            return new EvaluateTrainingResponse(
                    "training-evaluation-" + TRAINING_ID,
                    1,
                    new BigDecimal("84.25")
            );
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("프로필 집계 실패"))
                .when(studentFeatureProfileService)
                .recalculate(any());

        BigDecimal accuracy = trainingService.completeTraining(
                TEACHER_ID,
                STUDENT_ID,
                TRAINING_ID,
                objectMapper.createObjectNode(),
                LocalDateTime.of(2026, 7, 31, 11, 0)
        );

        assertThat(accuracy).isEqualByComparingTo("84.25");
        assertThat(trainingValue("status", String.class)).isEqualTo("COMPLETED");
        assertThat(trainingValue("result", String.class)).isNotNull();
        assertThat(trainingValue("accuracy", Integer.class)).isEqualTo(843);
        assertThat(trainingValue("started_at", LocalDateTime.class)).isNotNull();
        assertThat(trainingValue("finished_at", LocalDateTime.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_curriculums WHERE id = ?",
                String.class,
                CURRICULUM_ID
        )).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at FROM daily_curriculums WHERE id = ?",
                LocalDateTime.class,
                CURRICULUM_ID
        )).isNotNull();
        verify(studentFeatureProfileService).recalculate(any());
    }

    @Test
    void eventRegistrationFailureRollsBackTestResultTimeAndCurriculumStatus() {
        TestDataEntity testData = mock(TestDataEntity.class);
        when(testData.getGeneratedData()).thenReturn("""
                {"questions":[{"questionNo":1,"type":"CONSONANT_SOUND_CHOICE"}]}
                """);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(TEST_ID))
                .thenReturn(Optional.of(testData));
        org.mockito.Mockito.doThrow(new IllegalStateException("이벤트 등록 실패"))
                .when(realtimeEventPublisher)
                .publishAfterCommit(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> appTestService.complete(
                TEACHER_ID,
                STUDENT_ID,
                new TestCompleteRequest(TEST_ID)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이벤트 등록 실패");

        assertThat(testValue("status", String.class)).isEqualTo("IN_PROGRESS");
        assertThat(testValue("accuracy", BigDecimal.class)).isNull();
        assertThat(testValue("finished_at", LocalDateTime.class)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM test_curriculums WHERE id = ?",
                String.class,
                TEST_CURRICULUM_ID
        )).isEqualTo("IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at FROM test_curriculums WHERE id = ?",
                LocalDateTime.class,
                TEST_CURRICULUM_ID
        )).isNull();
    }

    private <T> T trainingValue(String column, Class<T> type) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM trainings WHERE id = ?",
                type,
                TRAINING_ID
        );
    }

    private <T> T testValue(String column, Class<T> type) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM tests WHERE id = ?",
                type,
                TEST_ID
        );
    }
}
