package com.iread.backend.training.generation;

import com.iread.backend.readingfeature.domain.ReadingFeatureCategory;
import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import com.iread.backend.readingfeature.domain.ReadingFeatureScope;
import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.analysis.KomoranMorphAnalyzer;
import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalizedTrainingGenerationServiceTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void createsFixedGeneratedDataEnvelopeWithoutStudentIdentity() throws Exception {
        TrainingEntity training = training(sentencePrompt());
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L))
                .thenReturn(List.of(profile(
                        training.getDailyCurriculum().getStudent(),
                        "SENTENCE.SIMPLE",
                        ReadingFeatureCategory.SENTENCE,
                        ReadingFeatureScope.SENTENCE
                )));
        DeterministicTrainingCandidateProvider delegate =
                new DeterministicTrainingCandidateProvider(objectMapper);
        TrainingCandidateProvider provider = mock(TrainingCandidateProvider.class);
        when(provider.generate(any())).thenAnswer(invocation ->
                delegate.generate(invocation.getArgument(0)));
        PersonalizedTrainingGenerationService service = service(provider, profiles);

        JsonNode generated = service.generate(training);

        assertThat(generated.path("schemaVersion").asInt()).isEqualTo(2);
        assertThat(generated.path("generationMetadata").path("provider").asText()).isEqualTo("MOCK");
        assertThat(generated.path("profileSnapshot").path("analysisVersion").asText())
                .isEqualTo("WEAKNESS_V1");
        assertThat(generated.path("profileSnapshot").path("features")).hasSize(1);
        assertThat(generated.path("questions")).hasSize(5);
        for (int index = 0; index < 5; index++) {
            JsonNode question = generated.path("questions").get(index);
            assertThat(question.path("questionNo").asInt()).isEqualTo(index + 1);
            assertThat(question.path("type").asText()).isEqualTo("SENTENCE_READING");
            assertThat(question.path("requiredInputs")).containsExactly(
                    objectMapper.getNodeFactory().textNode("VOICE"),
                    objectMapper.getNodeFactory().textNode("GAZE")
            );
            assertThat(question.path("content").isObject()).isTrue();
            assertThat(question.path("answer").isObject()).isTrue();
            assertThat(question.path("analysisTargets").isArray()).isTrue();
            assertThat(question.path("words").isArray()).isTrue();
        }
        assertThat(generated.path("validationResult").path("passed").asBoolean()).isTrue();
        assertThat(generated.toString())
                .doesNotContain("\"studentId\"")
                .doesNotContain("학습자");
        ArgumentCaptor<TrainingCandidateRequest> requestCaptor =
                ArgumentCaptor.forClass(TrainingCandidateRequest.class);
        verify(provider).generate(requestCaptor.capture());
        assertThat(objectMapper.writeValueAsString(requestCaptor.getValue()))
                .doesNotContain("\"studentId\"")
                .doesNotContain("\"studentName\"")
                .doesNotContain("\"birthday\"")
                .doesNotContain("\"guardianContact\"");
    }

    @Test
    void retriesThreeTimesAndReturnsNoPartialEnvelopeWhenTargetFeatureIsMissing() throws Exception {
        TrainingEntity training = training(sentencePrompt());
        StudentFeatureProfileRepository profiles = mock(StudentFeatureProfileRepository.class);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L))
                .thenReturn(List.of(profile(
                        training.getDailyCurriculum().getStudent(),
                        "PHONOLOGY.PALATALIZATION",
                        ReadingFeatureCategory.PHONOLOGY,
                        ReadingFeatureScope.WORD_BOUNDARY
                )));
        DeterministicTrainingCandidateProvider delegate =
                new DeterministicTrainingCandidateProvider(objectMapper);
        TrainingCandidateProvider provider = mock(TrainingCandidateProvider.class);
        when(provider.generate(any())).thenAnswer(invocation ->
                delegate.generate(invocation.getArgument(0)));
        PersonalizedTrainingGenerationService service = service(provider, profiles);

        assertThatThrownBy(() -> service.generate(training))
                .isInstanceOf(TrainingGenerationException.class)
                .hasMessageContaining("문항 5개");
        verify(provider, times(3)).generate(any());
    }

    private PersonalizedTrainingGenerationService service(
            TrainingCandidateProvider provider,
            StudentFeatureProfileRepository profiles
    ) {
        KoreanTextAnalyzer analyzer = new KoreanTextAnalyzer(
                new KomoranMorphAnalyzer(),
                new KoreanG2pEngine()
        );
        return new PersonalizedTrainingGenerationService(
                objectMapper,
                provider,
                new TrainingCandidateValidator(),
                new TrainingQuestionAssembler(objectMapper, analyzer),
                profiles
        );
    }

    private TrainingEntity training(String prompt) {
        StudentEntity student = StudentEntity.builder().name("테스트").build();
        ReflectionTestUtils.setField(student, "id", 15L);
        TrainingTemplateEntity template = instantiate(TrainingTemplateEntity.class);
        ReflectionTestUtils.setField(template, "id", 25L);
        ReflectionTestUtils.setField(template, "name", "문장 읽기");
        ReflectionTestUtils.setField(template, "prompt", prompt);
        ReflectionTestUtils.setField(template, "sequenceNo", 1);
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(student, List.of(template));
        TrainingEntity training = curriculum.getTrainings().getFirst();
        ReflectionTestUtils.setField(training, "id", 120L);
        return training;
    }

    private StudentFeatureProfileEntity profile(
            StudentEntity student,
            String code,
            ReadingFeatureCategory category,
            ReadingFeatureScope scope
    ) {
        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                500L,
                null,
                code,
                code,
                category,
                scope
        );
        StudentFeatureProfileEntity profile = new StudentFeatureProfileEntity(
                700L,
                student,
                feature,
                new BigDecimal("0.8000")
        );
        profile.updateMetrics(
                new BigDecimal("0.6500"),
                750,
                new BigDecimal("0.25"),
                900,
                new BigDecimal("2.00"),
                new BigDecimal("1.00"),
                new BigDecimal("0.10"),
                1800,
                800,
                new BigDecimal("0.8000"),
                10,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return profile;
    }

    private String sentencePrompt() throws Exception {
        JsonNode root;
        try (var input = getClass().getClassLoader().getResourceAsStream("training-templates.json")) {
            root = objectMapper.readTree(input);
        }
        for (JsonNode template : root.path("templates")) {
            if ("SENTENCE_READING".equals(
                    template.path("prompt").path("trainingType").asText()
            )) {
                return objectMapper.writeValueAsString(template.path("prompt"));
            }
        }
        throw new IllegalStateException("SENTENCE_READING seed가 없습니다.");
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", exception);
        }
    }
}
