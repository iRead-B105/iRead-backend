package com.iread.backend.test.admin.result;

import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestQuestionResultAssemblerTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private TestDataRepository testDataRepository;
    private WordAttemptLogRepository wordAttemptLogRepository;
    private TestQuestionResultAssembler assembler;

    @BeforeEach
    void setUp() {
        testDataRepository = mock(TestDataRepository.class);
        wordAttemptLogRepository = mock(WordAttemptLogRepository.class);
        assembler = new TestQuestionResultAssembler(
                testDataRepository,
                wordAttemptLogRepository,
                new AppLearningQuestionSupport(objectMapper),
                new TestScoreNormalizer(),
                new TestTrackResolver(),
                objectMapper
        );
    }

    @Test
    void joinsChoiceSubmissionAndPreservesMeasuredZero() {
        StudentTestEntity test = test(10L, 1, BigDecimal.valueOf(100), """
                {
                  "submissions":[{
                    "questionNo":1,
                    "response":{"selectedIndex":0},
                    "correct":true,
                    "totalScore":1000
                  }],
                  "solvingTimeSeconds":12,
                  "gazeDepartureCount":0
                }
                """);
        generated(test, """
                {"questions":[{
                  "questionNo":1,
                  "type":"CONSONANT_SOUND_CHOICE",
                  "content":{"instruction":"소리를 고르세요.","choices":["ㄱ","ㄴ"]},
                  "answer":{"answerIndex":0}
                }]}
                """);

        var result = assembler.assemble(test);

        assertThat(result.questionNo()).isEqualTo(1);
        assertThat(result.trackCode()).isEqualTo("phonological");
        assertThat(result.responseType()).isEqualTo("SINGLE_CHOICE");
        assertThat(result.selectedAnswer().asText()).isEqualTo("ㄱ");
        assertThat(result.correctAnswer().asText()).isEqualTo("ㄱ");
        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("100.00");
        assertThat(result.pronunciationScore()).isNull();
        assertThat(result.solvingTimeSeconds()).isEqualTo(12L);
        assertThat(result.gazeDepartureCount()).isZero();
    }

    @Test
    void keepsStructuredOrderingResponseInsteadOfFlatteningIt() {
        StudentTestEntity test = test(11L, 4, BigDecimal.valueOf(100), """
                {"submissions":[{
                  "questionNo":1,
                  "response":{"orderedIndexes":[1,0]},
                  "correct":true,
                  "totalScore":1000
                }]}
                """);
        generated(test, """
                {"questions":[{
                  "questionNo":1,
                  "type":"SENTENCE_ASSEMBLY",
                  "content":{"prompt":"문장을 만드세요."},
                  "answer":{"answerOrder":[1,0]}
                }]}
                """);

        var result = assembler.assemble(test);

        assertThat(result.trackCode()).isEqualTo("short-text");
        assertThat(result.responseType()).isEqualTo("ORDERING");
        assertThat(result.selectedAnswer()).isEqualTo(result.correctAnswer());
        assertThat(result.selectedAnswer().isArray()).isTrue();
    }

    @Test
    void usesFinalAudioAttemptsAndLatestPronunciationAnalysis() {
        StudentTestEntity test = test(12L, 7, BigDecimal.valueOf(80), """
                {
                  "pronunciationAnalyses":[
                    {"questionNo":1,"attemptNo":1,"referenceText":"이전 문장","pronunciationAccuracyScore":70,"passed":false},
                    {"questionNo":1,"attemptNo":2,"referenceText":"바른 문장","pronunciationAccuracyScore":87.5,"passed":true}
                  ],
                  "solvingTimeSeconds":20
                }
                """);
        generated(test, """
                {"questions":[{
                  "questionNo":1,
                  "type":"SENTENCE_READING",
                  "text":"바른 문장",
                  "analysisTargets":[{"text":"바른 문장"}]
                }]}
                """);
        WordAttemptLogEntity attempt = mock(WordAttemptLogEntity.class);
        when(attempt.getSurfaceText()).thenReturn("바른 문장");
        when(attempt.getTotalScore()).thenReturn(800);
        when(attempt.getPronunciationAccuracyScore()).thenReturn(82);
        when(attempt.getCorrect()).thenReturn(true);
        when(wordAttemptLogRepository.findAllByTestIdAndQuestionNoAndFinalAttemptTrue(12L, 1))
                .thenReturn(List.of(attempt));

        var result = assembler.assemble(test);

        assertThat(result.trackCode()).isEqualTo("fluency");
        assertThat(result.responseType()).isEqualTo("AUDIO");
        assertThat(result.selectedAnswer().isNull()).isTrue();
        assertThat(result.correctAnswer().asText()).isEqualTo("바른 문장");
        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("80.00");
        assertThat(result.pronunciationScore()).isEqualByComparingTo("87.50");
        assertThat(result.gazeDepartureCount()).isNull();
    }

    @Test
    void expandsLegacyMultiQuestionTestIntoGlobalSequence() {
        StudentTestEntity test = test(13L, 2, BigDecimal.valueOf(62), """
                {
                  "questions":[
                    {"questionNumber":1,"question":"첫 문항","selectedAnswer":"학생 답 1","correctAnswer":"정답 1","isCorrect":true},
                    {"questionNumber":2,"question":"둘째 문항","selectedAnswer":"학생 답 2","correctAnswer":"정답 2","isCorrect":false},
                    {"questionNumber":3,"question":"셋째 문항","selectedAnswer":"학생 답 3","correctAnswer":"정답 3","isCorrect":true}
                  ],
                  "solvingTimeSeconds":30,
                  "gazeDepartureCount":2
                }
                """);
        generated(test, """
                {"questions":[
                  {"questionNo":1,"type":"SENTENCE_READING","analysisTargets":[{"text":"정답 1"}]},
                  {"questionNo":2,"type":"SENTENCE_READING","analysisTargets":[{"text":"정답 2"}]},
                  {"questionNo":3,"type":"SENTENCE_READING","analysisTargets":[{"text":"정답 3"}]}
                ]}
                """);

        var results = assembler.assembleAll(test);

        assertThat(results).extracting(
                result -> result.questionNo()
        ).containsExactly(1, 2, 3);
        assertThat(results).extracting(
                result -> result.sequenceNo()
        ).containsExactly(4, 5, 6);
        assertThat(results).extracting(
                result -> result.question()
        ).containsExactly("첫 문항", "둘째 문항", "셋째 문항");
        assertThat(results.get(1).selectedAnswer().asText()).isEqualTo("학생 답 2");
        assertThat(results.get(1).correctAnswer().asText()).isEqualTo("정답 2");
        assertThat(results.get(1).correct()).isFalse();
        assertThat(results).extracting(
                result -> result.solvingTimeSeconds()
        ).containsExactly(30L, null, null);
        assertThat(results).extracting(
                result -> result.gazeDepartureCount()
        ).containsExactly(2, null, null);
    }

    @Test
    void assignsQuestionNoFromOneBasedIndexForLegacyGeneratedQuestions() {
        StudentTestEntity test = test(14L, 1, BigDecimal.valueOf(50), "{}");
        generated(test, """
                {"questions":[
                  {"type":"SENTENCE_READING","text":"first"},
                  {"type":"SENTENCE_READING","text":"second"},
                  {"type":"SENTENCE_READING","text":"third"}
                ]}
                """);

        var results = assembler.assembleAll(test);

        assertThat(results).extracting(result -> result.questionNo())
                .containsExactly(1, 2, 3);
    }

    @Test
    void returnsNullWhenQuestionOriginalIsMissing() {
        StudentTestEntity test = test(15L, 1, BigDecimal.valueOf(0), "{}");
        generated(test, """
                {"questions":[{
                  "questionNo":1,
                  "type":"CONSONANT_SOUND_CHOICE",
                  "content":{"choices":["a","b"]},
                  "answer":{"answerIndex":0}
                }]}
                """);

        var result = assembler.assemble(test);

        assertThat(result.question()).isNull();
    }

    @Test
    void rejectsDuplicateQuestionNoWithinOneTest() {
        StudentTestEntity test = test(16L, 1, BigDecimal.valueOf(0), "{}");
        generated(test, """
                {"questions":[
                  {"questionNo":1,"type":"SENTENCE_READING","text":"first"},
                  {"questionNo":1,"type":"SENTENCE_READING","text":"second"}
                ]}
                """);

        assertThatThrownBy(() -> assembler.assembleAll(test))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate test questionNo");
    }

    private StudentTestEntity test(Long id, int sequenceNo, BigDecimal accuracy, String result) {
        StudentTestEntity test = mock(StudentTestEntity.class);
        when(test.getId()).thenReturn(id);
        when(test.getSequenceNo()).thenReturn(sequenceNo);
        when(test.getAccuracy()).thenReturn(accuracy);
        when(test.getResult()).thenReturn(result);
        return test;
    }

    private void generated(StudentTestEntity test, String generatedData) {
        Long testId = test.getId();
        TestDataEntity data = new TestDataEntity(
                testId,
                test,
                generatedData,
                LocalDateTime.now()
        );
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(testId))
                .thenReturn(Optional.of(data));
    }
}
