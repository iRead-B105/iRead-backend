package com.iread.backend.training.admin.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.*;
import com.iread.backend.training.admin.dto.req.ExpectedWordRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock DailyCurriculumRepository dailyCurriculumRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock TrainingTemplateRepository trainingTemplateRepository;
    @Mock TrainingDataRepository trainingDataRepository;
    @Mock WordRepository wordRepository;
    @Mock AiClient aiClient;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService(
                studentRepository,
                dailyCurriculumRepository,
                trainingRepository,
                trainingTemplateRepository,
                trainingDataRepository,
                wordRepository,
                aiClient,
                JsonMapper.builder().build()
        );
    }

    @Test
    void 커리큘럼_달성률은_훈련_정답률의_평균이다() {
        DailyCurriculumEntity curriculum = curriculum(100L);
        TrainingEntity first = training(1L, curriculum, template(11L, "훈련1"), new BigDecimal("80.00"));
        TrainingEntity second = training(2L, curriculum, template(12L, "훈련2"), new BigDecimal("90.00"));
        curriculum.getTrainings().addAll(List.of(first, second));
        ReflectionTestUtils.setField(curriculum, "completedAt", LocalDateTime.of(2026, 7, 20, 12, 0));
        allowStudent();
        when(dailyCurriculumRepository.findAllByStudentIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(10L))
                .thenReturn(List.of(curriculum));

        var result = trainingService.getCurriculumLogs(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().achievement()).isEqualByComparingTo("85.00");
        assertThat(result.getFirst().trainings()).hasSize(2);
    }

    @Test
    void 훈련_result_JSON을_문항별_이력으로_변환한다() {
        DailyCurriculumEntity curriculum = curriculum(100L);
        TrainingEntity training = training(1L, curriculum, template(11L, "단어 훈련"), new BigDecimal("50.00"));
        ReflectionTestUtils.setField(training, "result", """
                {"version":1,"questions":[
                  {"questionNumber":1,"wordId":101,"question":"사과","isCorrect":false,
                   "correctAnswer":"사과","selectedAnswer":"사가"}
                ]}
                """);
        curriculum.getTrainings().add(training);
        allowStudent();
        when(dailyCurriculumRepository.findByIdAndStudentId(100L, 10L)).thenReturn(Optional.of(curriculum));

        var result = trainingService.getTrainingLog(1L, 10L, 100L);

        var question = result.trainings().getFirst().questions().getFirst();
        assertThat(question.questionNumber()).isEqualTo(1);
        assertThat(question.wordId()).isEqualTo(101L);
        assertThat(question.correct()).isFalse();
        assertThat(question.correctAnswer()).isEqualTo("사과");
        assertThat(question.selectedAnswer()).isEqualTo("사가");
    }

    @Test
    void 진행중인_훈련이_있으면_차회_커리큘럼을_수정할_수_없다() {
        DailyCurriculumEntity curriculum = curriculum(100L);
        TrainingEntity training = training(1L, curriculum, template(11L, "훈련"), null);
        ReflectionTestUtils.setField(training, "status", TrainingStatus.IN_PROGRESS);
        curriculum.getTrainings().add(training);
        allowStudent();
        when(dailyCurriculumRepository.findByIdAndStudentId(100L, 10L)).thenReturn(Optional.of(curriculum));

        assertThatThrownBy(() -> trainingService.updateDailyCurriculum(
                1L, 10L, 100L, new UpdateCurriculumRequest(List.of(11L))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("시작했거나 완료된 커리큘럼은 수정할 수 없습니다.");
        verify(trainingTemplateRepository, never()).findAllById(any());
    }

    @Test
    void 훈련_템플릿_ID로_일일_커리큘럼을_생성한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        List<TrainingTemplateEntity> templates = List.of(
                template(11L, "훈련1"),
                template(12L, "훈련2")
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(10L, DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(Optional.empty());
        when(trainingTemplateRepository.findAllById(List.of(11L, 12L))).thenReturn(templates);
        when(dailyCurriculumRepository.saveAndFlush(any(DailyCurriculumEntity.class)))
                .thenAnswer(invocation -> {
                    DailyCurriculumEntity saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 100L);
                    ReflectionTestUtils.setField(saved.getTrainings().get(0), "id", 1L);
                    ReflectionTestUtils.setField(saved.getTrainings().get(1), "id", 2L);
                    return saved;
                });

        var result = trainingService.createDailyCurriculum(
                1L,
                10L,
                new UpdateCurriculumRequest(List.of(11L, 12L))
        );

        assertThat(result.curriculumId()).isEqualTo(100L);
        assertThat(result.trainings()).extracting("trainingId").containsExactly(1L, 2L);
        assertThat(result.trainings()).extracting("trainingTemplateId").containsExactly(11L, 12L);
    }

    @Test
    void 수정_가능한_커리큘럼은_한_개만_생성할_수_있다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(dailyCurriculumRepository.findByStudentIdAndStatus(10L, DailyCurriculumStatus.NOT_STARTED))
                .thenReturn(Optional.of(curriculum(100L)));

        assertThatThrownBy(() -> trainingService.createDailyCurriculum(
                1L,
                10L,
                new UpdateCurriculumRequest(List.of(11L))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수정 가능한 커리큘럼은 한 개만 생성할 수 있습니다.");
        verify(trainingTemplateRepository, never()).findAllById(any());
    }

    @Test
    void 예정_단어가_없으면_단어를_생성하고_JSON에_추가한다() {
        TrainingEntity training = ownedTraining(1L);
        TrainingDataEntity data = new TrainingDataEntity(
                training, "{\"version\":1,\"expectedWords\":[],\"content\":{}}"
        );
        WordEntity word = new WordEntity("사과");
        ReflectionTestUtils.setField(word, "id", 101L);
        allowStudent();
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(1L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(1L)).thenReturn(Optional.of(data));
        when(wordRepository.findByContent("사과")).thenReturn(Optional.empty());
        when(wordRepository.save(any(WordEntity.class))).thenReturn(word);

        trainingService.addExpectedWord(1L, 10L, 1L, new ExpectedWordRequest("사과"));

        assertThat(data.getGeneratedData()).contains("\"wordId\":101").contains("\"wordName\":\"사과\"");
        assertThat(training.getStatus()).isEqualTo(TrainingStatus.NOT_READY);
    }

    @Test
    void 예정_단어는_중복해서_추가할_수_없다() {
        TrainingEntity training = ownedTraining(1L);
        TrainingDataEntity data = new TrainingDataEntity(
                training, "{\"version\":1,\"expectedWords\":[{\"wordId\":101,\"wordName\":\"사과\"}]}"
        );
        allowStudent();
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(1L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(1L)).thenReturn(Optional.of(data));

        assertThatThrownBy(() -> trainingService.addExpectedWord(
                1L, 10L, 1L, new ExpectedWordRequest("사과")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 추가된 예정 단어입니다.");
        verify(wordRepository, never()).save(any());
    }

    @Test
    void 모든_훈련이_완료되면_일일커리큘럼의_완료일을_기록한다() {
        DailyCurriculumEntity curriculum = curriculum(100L);
        TrainingEntity first = training(1L, curriculum, template(11L, "훈련1"), null);
        TrainingEntity second = training(2L, curriculum, template(12L, "훈련2"), null);
        curriculum.getTrainings().addAll(List.of(first, second));
        LocalDateTime firstFinishedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        LocalDateTime lastFinishedAt = LocalDateTime.of(2026, 7, 20, 10, 30);

        first.complete("{}", new BigDecimal("80"), firstFinishedAt);
        assertThat(curriculum.getCompletedAt()).isNull();
        second.complete("{}", new BigDecimal("90"), lastFinishedAt);

        assertThat(curriculum.getStatus()).isEqualTo(DailyCurriculumStatus.COMPLETED);
        assertThat(curriculum.getCompletedAt()).isEqualTo(lastFinishedAt);
    }

    @Test
    void catalogAchievementUsesMaximumAccuracyForSameTemplate() {
        TrainingTemplateEntity template = template(11L, "word training");
        TrainingEntity lower = training(1L, curriculum(100L), template, new BigDecimal("72.50"));
        TrainingEntity higher = training(2L, curriculum(101L), template, new BigDecimal("91.25"));
        ReflectionTestUtils.setField(lower, "status", TrainingStatus.COMPLETED);
        ReflectionTestUtils.setField(higher, "status", TrainingStatus.COMPLETED);
        allowStudent();
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(10L, TrainingStatus.COMPLETED))
                .thenReturn(List.of(lower, higher));
        when(trainingTemplateRepository.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(List.of(template));

        var result = trainingService.getTrainingCatalog(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().studentAchievement()).isEqualByComparingTo("91.25");
    }

    @Test
    void completeTrainingStoresAiAccuracyAndResultJson() throws Exception {
        TrainingEntity training = ownedTraining(1L);
        ReflectionTestUtils.setField(training, "status", TrainingStatus.NOT_STARTED);
        training.getDailyCurriculum().getTrainings().add(training);
        allowStudent();
        when(trainingRepository.findForUpdate(1L, 10L)).thenReturn(Optional.of(training));
        when(aiClient.evaluateTraining(any(EvaluateTrainingRequest.class)))
                .thenReturn(new EvaluateTrainingResponse(
                        "training-evaluation-1", 1, new BigDecimal("87.456")
                ));
        var resultJson = JsonMapper.builder().build().readTree("""
                {"questions":[{"questionId":"q1","selectedAnswer":"apple"}]}
                """);

        BigDecimal accuracy = trainingService.completeTraining(1L, 10L, 1L, resultJson);

        assertThat(accuracy).isEqualByComparingTo("87.46");
        assertThat(training.getAccuracy()).isEqualByComparingTo("87.46");
        assertThat(training.getResult()).contains("\"questionId\":\"q1\"");
        assertThat(training.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
        ArgumentCaptor<EvaluateTrainingRequest> captor = ArgumentCaptor.forClass(EvaluateTrainingRequest.class);
        verify(aiClient).evaluateTraining(captor.capture());
        assertThat(captor.getValue().requestId()).isEqualTo("training-evaluation-1");
        assertThat(captor.getValue().result()).isEqualTo(resultJson);
    }

    @Test
    void completedTrainingReturnsStoredAccuracyWithoutCallingAiAgain() throws Exception {
        TrainingEntity training = ownedTraining(1L);
        ReflectionTestUtils.setField(training, "status", TrainingStatus.COMPLETED);
        ReflectionTestUtils.setField(training, "accuracy", new BigDecimal("93.00"));
        allowStudent();
        when(trainingRepository.findForUpdate(1L, 10L)).thenReturn(Optional.of(training));
        var resultJson = JsonMapper.builder().build().readTree("{}");

        BigDecimal accuracy = trainingService.completeTraining(1L, 10L, 1L, resultJson);

        assertThat(accuracy).isEqualByComparingTo("93.00");
        verify(aiClient, never()).evaluateTraining(any());
    }

    private void allowStudent() {
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(StudentEntity.class)));
    }

    private DailyCurriculumEntity curriculum(Long id) {
        DailyCurriculumEntity entity = instantiate(DailyCurriculumEntity.class);
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "trainings", new java.util.ArrayList<TrainingEntity>());
        return entity;
    }

    private TrainingTemplateEntity template(Long id, String name) {
        CurriculumUnitEntity unit = instantiate(CurriculumUnitEntity.class);
        ReflectionTestUtils.setField(unit, "unitName", "단원");
        TrainingTemplateEntity template = instantiate(TrainingTemplateEntity.class);
        ReflectionTestUtils.setField(template, "id", id);
        ReflectionTestUtils.setField(template, "name", name);
        ReflectionTestUtils.setField(template, "curriculumUnit", unit);
        return template;
    }

    private TrainingEntity training(Long id, DailyCurriculumEntity curriculum,
                                    TrainingTemplateEntity template, BigDecimal accuracy) {
        TrainingEntity training = instantiate(TrainingEntity.class);
        ReflectionTestUtils.setField(training, "id", id);
        ReflectionTestUtils.setField(training, "dailyCurriculum", curriculum);
        ReflectionTestUtils.setField(training, "trainingTemplate", template);
        ReflectionTestUtils.setField(training, "accuracy", accuracy);
        ReflectionTestUtils.setField(training, "status", TrainingStatus.NOT_READY);
        return training;
    }

    private TrainingEntity ownedTraining(Long id) {
        return training(id, curriculum(100L), template(11L, "훈련"), null);
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
