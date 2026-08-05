package com.iread.backend.training.admin.result;

import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingQuestionResultAssemblerTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private TrainingDataRepository trainingDataRepository;
    private WordAttemptLogRepository wordAttemptLogRepository;
    private TrainingQuestionResultAssembler assembler;

    @BeforeEach
    void setUp() {
        trainingDataRepository = mock(TrainingDataRepository.class);
        wordAttemptLogRepository = mock(WordAttemptLogRepository.class);
        assembler = new TrainingQuestionResultAssembler(
                trainingDataRepository,
                wordAttemptLogRepository,
                new AppLearningQuestionSupport(objectMapper),
                objectMapper
        );
    }

    @Test
    void joinsCorrectIncorrectAndUnsubmittedQuestionsWithoutDroppingDetails() {
        TrainingEntity training = training(1L, """
                {
                  "submissions":[
                    {"questionNo":1,"responseType":"SINGLE_CHOICE",
                     "response":{"selectedIndex":0},"correct":true,"totalScore":1000},
                    {"questionNo":2,"responseType":"SINGLE_CHOICE",
                     "response":{"selectedIndex":0},"correct":false,"totalScore":0}
                  ],
                  "questions":[
                    {"questionNo":1,"isCorrect":true,"totalScore":1000},
                    {"questionNo":2,"isCorrect":false,"totalScore":0}
                  ]
                }
                """);
        generated(training, """
                {"questions":[
                  {"questionNo":1,"type":"CONSONANT_SOUND_CHOICE",
                   "content":{"instruction":"첫 소리를 고르세요.","choices":["ㄱ","ㄴ"]},
                   "answer":{"answerIndex":0}},
                  {"questionNo":2,"type":"VOWEL_SOUND_CHOICE",
                   "content":{"instruction":"모음을 고르세요.","choices":["ㅏ","ㅓ"]},
                   "answer":{"answerIndex":1}},
                  {"questionNo":3,"type":"WORD_INITIAL_CHOICE",
                   "content":{"instruction":"첫소리를 고르세요.","choices":["ㅂ","ㅅ"]},
                   "answer":{"answerIndex":0}}
                ]}
                """);

        var results = assembler.assembleAll(training);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(result -> result.questionNo())
                .containsExactly(1, 2, 3);
        assertThat(results.get(0).question().path("instruction").asText())
                .isEqualTo("첫 소리를 고르세요.");
        assertThat(results.get(0).selectedAnswer().asText()).isEqualTo("ㄱ");
        assertThat(results.get(0).correctAnswer().asText()).isEqualTo("ㄱ");
        assertThat(results.get(0).correct()).isTrue();
        assertThat(results.get(0).score()).isEqualByComparingTo("100.00");
        assertThat(results.get(1).selectedAnswer().asText()).isEqualTo("ㅏ");
        assertThat(results.get(1).correctAnswer().asText()).isEqualTo("ㅓ");
        assertThat(results.get(1).correct()).isFalse();
        assertThat(results.get(1).score()).isEqualByComparingTo("0.00");
        assertThat(results.get(2).selectedAnswer().isNull()).isTrue();
        assertThat(results.get(2).correctAnswer().asText()).isEqualTo("ㅂ");
        assertThat(results.get(2).correct()).isNull();
        assertThat(results.get(2).score()).isNull();
    }

    @Test
    void preservesStructuredAnswersForEveryNonAudioResponseType() {
        TrainingEntity training = training(2L, """
                {"submissions":[
                  {"questionNo":1,"responseType":"TRACE",
                   "response":{"canvasWidth":100,"canvasHeight":100,
                   "strokes":[{"points":[{"x":1,"y":2},{"x":3,"y":4}]}]},
                   "correct":true,"totalScore":1000},
                  {"questionNo":2,"responseType":"ORDERING",
                   "response":{"orderedIndexes":[1,0]},"correct":true,"totalScore":1000},
                  {"questionNo":3,"responseType":"COMPONENT_BUILD",
                   "response":{"selections":[
                     {"slot":"INITIAL","selectedIndex":1},
                     {"slot":"MEDIAL","selectedIndex":2}]},
                   "correct":true,"totalScore":1000},
                  {"questionNo":4,"responseType":"TEXT_INPUT",
                   "response":{"text":"사과"},"correct":true,"totalScore":1000}
                ]}
                """);
        generated(training, """
                {"questions":[
                  {"questionNo":1,"type":"VOWEL_TRACE",
                   "content":{"instruction":"따라 쓰세요."},"answer":{"target":"ㅏ"}},
                  {"questionNo":2,"type":"SENTENCE_ASSEMBLY",
                   "content":{"prompt":"순서를 맞추세요."},"answer":{"answerOrder":[1,0]}},
                  {"questionNo":3,"type":"BASIC_SYLLABLE_BUILD",
                   "content":{"prompt":"글자를 만드세요."},
                   "answer":{"initialAnswerIndex":1,"medialAnswerIndex":2}},
                  {"questionNo":4,"type":"FILL_IN_THE_BLANK",
                   "content":{"inputType":"TEXT","prompt":"빈칸을 채우세요."},
                   "answer":{"acceptedAnswers":["사과"]}}
                ]}
                """);

        var results = assembler.assembleAll(training);

        assertThat(results).extracting(result -> result.responseType())
                .containsExactly("TRACE", "ORDERING", "COMPONENT_BUILD", "TEXT_INPUT");
        assertThat(results.get(0).selectedAnswer().isObject()).isTrue();
        assertThat(results.get(1).selectedAnswer().isArray()).isTrue();
        assertThat(results.get(1).selectedAnswer()).isEqualTo(results.get(1).correctAnswer());
        assertThat(results.get(2).selectedAnswer().isArray()).isTrue();
        assertThat(results.get(2).correctAnswer().path("INITIAL").asInt()).isEqualTo(1);
        assertThat(results.get(3).selectedAnswer().asText()).isEqualTo("사과");
        assertThat(results.get(3).correctAnswer().asText()).isEqualTo("사과");
    }

    @Test
    void usesFinalAudioAttemptsForCorrectnessAndScoreWithoutInventingTranscript() {
        TrainingEntity training = training(3L, "{}");
        generated(training, """
                {"questions":[{
                  "questionNo":1,"type":"WORD_READING","text":"바다",
                  "analysisTargets":[{"text":"바다"}]
                }]}
                """);
        WordAttemptLogEntity first = attempt(true, 900);
        WordAttemptLogEntity second = attempt(true, 700);
        when(wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndFinalAttemptTrue(3L, 1))
                .thenReturn(List.of(first, second));

        var result = assembler.assembleAll(training).getFirst();

        assertThat(result.responseType()).isEqualTo("AUDIO");
        assertThat(result.question().path("text").asText()).isEqualTo("바다");
        assertThat(result.selectedAnswer().isNull()).isTrue();
        assertThat(result.correctAnswer().asText()).isEqualTo("바다");
        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("80.00");
    }

    @Test
    void fallsBackToLegacyQuestionDetailsWhenGeneratedSourceIsMissing() {
        TrainingEntity training = training(4L, """
                {"questions":[{
                  "questionNumber":1,"question":"사과를 읽으세요.",
                  "selectedAnswer":"사가","correctAnswer":"사과","isCorrect":false,
                  "score":0
                }]}
                """);

        var result = assembler.assembleAll(training).getFirst();

        assertThat(result.questionNo()).isEqualTo(1);
        assertThat(result.question().path("text").asText()).isEqualTo("사과를 읽으세요.");
        assertThat(result.selectedAnswer().asText()).isEqualTo("사가");
        assertThat(result.correctAnswer().asText()).isEqualTo("사과");
        assertThat(result.correct()).isFalse();
        assertThat(result.score()).isEqualByComparingTo("0.00");
    }

    private TrainingEntity training(Long id, String result) {
        TrainingEntity training = mock(TrainingEntity.class);
        when(training.getId()).thenReturn(id);
        when(training.getResult()).thenReturn(result);
        return training;
    }

    private void generated(TrainingEntity training, String generatedData) {
        when(trainingDataRepository.findByTrainingId(training.getId()))
                .thenReturn(Optional.of(new TrainingDataEntity(training, generatedData)));
    }

    private WordAttemptLogEntity attempt(Boolean correct, Integer totalScore) {
        WordAttemptLogEntity attempt = mock(WordAttemptLogEntity.class);
        when(attempt.getCorrect()).thenReturn(correct);
        when(attempt.getTotalScore()).thenReturn(totalScore);
        return attempt;
    }
}
