package com.iread.backend.training.admin.service;

import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.dto.req.UpdateLessonMaterialRequest;
import com.iread.backend.training.admin.exception.LessonMaterialException;
import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.domain.CurriculumUnitEntity;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.TrainingCandidateValidator;
import com.iread.backend.training.generation.TrainingQuestionAssembler;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonMaterialServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock TrainingDataRepository trainingDataRepository;
    @Mock RealtimeEventPublisher realtimeEventPublisher;

    private JsonMapper objectMapper;
    private LessonMaterialService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        KoreanTextAnalyzer analyzer = new KoreanTextAnalyzer(new KoreanG2pEngine());
        service = new LessonMaterialService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                new TrainingCandidateValidator(),
                new TrainingQuestionAssembler(objectMapper, analyzer),
                objectMapper,
                realtimeEventPublisher
        );
    }

    @Test
    void returnsEmptyLessonMaterialWithRevisionZeroWhenGeneratedDataIsMissing() {
        TrainingEntity training = training();
        allowStudent();
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(31L, 10L))
                .thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(31L)).thenReturn(Optional.empty());

        var response = service.getLessonMaterial(1L, 10L, 31L);

        assertThat(response.trainingId()).isEqualTo(31L);
        assertThat(response.revision()).isZero();
        assertThat(response.editable()).isTrue();
        assertThat(response.materials()).isEmpty();
    }

    @Test
    void savesAllMaterialsPreservingInternalFieldsAndExpectedWords()
            throws Exception {
        TrainingEntity training = training();
        TrainingDataEntity data = new TrainingDataEntity(training, """
                {
                  "schemaVersion": 2,
                  "revision": 2,
                  "expectedWords": [{"wordId": 7, "wordName": "tree"}],
                  "profileSnapshot": {"analysisVersion": "WEAKNESS_V1", "features": []},
                  "questions": [{
                    "questionNo": 1,
                    "type": "WORD_READING",
                    "requiredInputs": ["VOICE", "GAZE"],
                    "content": {"words": ["tree"]},
                    "answer": {"expectedText": "tree"},
                    "analysisTargets": [],
                    "targetFeatureCodes": []
                  }]
                }
                """);
        allowStudent();
        when(trainingRepository.findForUpdate(31L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(31L)).thenReturn(Optional.of(data));

        UpdateLessonMaterialRequest request = new UpdateLessonMaterialRequest(
                2,
                List.of(new UpdateLessonMaterialRequest.Material(
                        1,
                        "WORD_READING",
                        objectMapper.readTree("""
                                {"instruction":"Read the word."}
                                """),
                        objectMapper.readTree("""
                                {"words":["tree"]}
                                """),
                        objectMapper.readTree("""
                                {"expectedText":"tree"}
                                """)
                ))
        );

        var response = service.updateLessonMaterial(1L, 10L, 31L, request);

        JsonNode saved = objectMapper.readTree(data.getGeneratedData());
        assertThat(response.revision()).isEqualTo(3);
        assertThat(response.source()).isEqualTo("MANUAL");
        assertThat(response.materials()).hasSize(1);
        assertThat(saved.path("revision").asInt()).isEqualTo(3);
        assertThat(saved.path("generationMetadata").path("source").asText())
                .isEqualTo("MANUAL");
        assertThat(saved.path("expectedWords").path(0).path("wordName").asText())
                .isEqualTo("tree");
        assertThat(saved.path("profileSnapshot").path("analysisVersion").asText())
                .isEqualTo("WEAKNESS_V1");
        assertThat(saved.path("questions").path(0).path("presentation")
                .path("instruction").asText()).isEqualTo("Read the word.");
        assertThat(training.getStatus().name()).isEqualTo("NOT_STARTED");
        verify(realtimeEventPublisher).publishAfterCommit(
                1L, 10L, RealtimeResource.TRAINING, 31L, "CONTENT_UPDATED"
        );
    }

    @Test
    void rejectsStaleRevisionWithConflict() {
        TrainingEntity training = training();
        TrainingDataEntity data = new TrainingDataEntity(
                training,
                "{\"schemaVersion\":2,\"revision\":4,\"questions\":[]}"
        );
        allowStudent();
        when(trainingRepository.findForUpdate(31L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(31L)).thenReturn(Optional.of(data));
        UpdateLessonMaterialRequest request = new UpdateLessonMaterialRequest(
                3,
                List.of(material())
        );

        assertThatThrownBy(() -> service.updateLessonMaterial(1L, 10L, 31L, request))
                .isInstanceOfSatisfying(LessonMaterialException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                    assertThat(exception.getCode())
                            .isEqualTo("LESSON_MATERIAL_REVISION_CONFLICT");
                });
    }

    @Test
    void rejectsChangedQuestionTypeWithValidationError() {
        TrainingEntity training = training();
        TrainingDataEntity data = new TrainingDataEntity(
                training,
                "{\"schemaVersion\":2,\"revision\":0,\"questions\":[]}"
        );
        allowStudent();
        when(trainingRepository.findForUpdate(31L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(31L)).thenReturn(Optional.of(data));
        UpdateLessonMaterialRequest.Material material = new UpdateLessonMaterialRequest.Material(
                1,
                "SENTENCE_READING",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode().putArray("words").add("tree"),
                objectMapper.createObjectNode().put("expectedText", "tree")
        );

        assertThatThrownBy(() -> service.updateLessonMaterial(
                1L,
                10L,
                31L,
                new UpdateLessonMaterialRequest(0, List.of(material))
        )).isInstanceOfSatisfying(LessonMaterialException.class, exception -> {
            assertThat(exception.getStatus().value()).isEqualTo(422);
            assertThat(exception.getCode()).isEqualTo("LESSON_MATERIAL_VALIDATION_FAILED");
        });
    }


    @Test
    void rejectsQuestionNumberThatDoesNotMatchMaterialOrder() {
        TrainingEntity training = training();
        TrainingDataEntity data = new TrainingDataEntity(
                training,
                "{\"schemaVersion\":2,\"revision\":0,\"questions\":[]}"
        );
        allowStudent();
        when(trainingRepository.findForUpdate(31L, 10L)).thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(31L)).thenReturn(Optional.of(data));
        UpdateLessonMaterialRequest.Material material = new UpdateLessonMaterialRequest.Material(
                2,
                "WORD_READING",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode().putArray("words").add("tree"),
                objectMapper.createObjectNode().put("expectedText", "tree")
        );

        assertThatThrownBy(() -> service.updateLessonMaterial(
                1L,
                10L,
                31L,
                new UpdateLessonMaterialRequest(0, List.of(material))
        )).isInstanceOfSatisfying(LessonMaterialException.class, exception -> {
            assertThat(exception.getStatus().value()).isEqualTo(422);
            assertThat(exception.getDetails().toString())
                    .contains("QUESTION_ORDER_MISMATCH");
        });
    }
    private void allowStudent() {
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(StudentEntity.builder().name("student").build()));
    }

    private UpdateLessonMaterialRequest.Material material() {
        return new UpdateLessonMaterialRequest.Material(
                1,
                "WORD_READING",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode().putArray("words").add("tree"),
                objectMapper.createObjectNode().put("expectedText", "tree")
        );
    }

    private TrainingEntity training() {
        StudentEntity student = StudentEntity.builder().name("student").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        CurriculumUnitEntity unit = instantiate(CurriculumUnitEntity.class);
        ReflectionTestUtils.setField(unit, "unitName", "word reading");
        TrainingTemplateEntity template = instantiate(TrainingTemplateEntity.class);
        ReflectionTestUtils.setField(template, "id", 21L);
        ReflectionTestUtils.setField(template, "curriculumUnit", unit);
        ReflectionTestUtils.setField(template, "name", "word reading");
        ReflectionTestUtils.setField(template, "sequenceNo", 1);
        ReflectionTestUtils.setField(template, "prompt", """
                {
                  "trainingType":"WORD_READING",
                  "requiredInputs":["VOICE","GAZE"],
                  "excludedFeatures":[],
                  "outputTemplate":{"type":"WORD_READING","data":[{"words":["<string>"]}]}
                }
                """);
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(student, List.of(template));
        TrainingEntity training = curriculum.getTrainings().getFirst();
        ReflectionTestUtils.setField(training, "id", 31L);
        return training;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create test entity.", exception);
        }
    }
}
