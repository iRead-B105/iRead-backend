package com.iread.backend.training.input;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingInputRequirementServiceTest {

    @Mock TrainingDataRepository trainingDataRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock GazeSessionRepository gazeSessionRepository;

    private TrainingInputRequirementService service;

    @BeforeEach
    void setUp() {
        service = new TrainingInputRequirementService(
                trainingDataRepository,
                wordAttemptLogRepository,
                gazeSessionRepository,
                JsonMapper.builder().build()
        );
    }

    @Test
    void rejectsRecordingForSelectionOnlyQuestion() {
        generatedData("""
                {
                  "questions":[{
                    "questionNo":1,
                    "type":"CONSONANT_SOUND_CHOICE",
                    "requiredInputs":[]
                  }]
                }
                """);

        assertThatThrownBy(() -> service.requireQuestionInput(
                30L,
                1,
                TrainingInputType.VOICE
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("VOICE 입력을 사용하지 않습니다");
    }

    @Test
    void requiresOneFinalRecordingPerVoiceQuestion() {
        generatedData(readingQuestions());
        when(wordAttemptLogRepository
                .existsByTrainingIdAndQuestionNoAndFinalAttemptTrueAndHasAudioDataTrueAndPronunciationAccuracyScoreIsNotNull(
                        30L,
                        1
                )).thenReturn(true);
        when(wordAttemptLogRepository
                .existsByTrainingIdAndQuestionNoAndFinalAttemptTrueAndHasAudioDataTrueAndPronunciationAccuracyScoreIsNotNull(
                        30L,
                        2
                )).thenReturn(false);

        assertThatThrownBy(() -> service.validateCompletion(30L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("[2]");
    }

    @Test
    void acceptsCompletionAfterVoiceAndGazeInputsExist() {
        generatedData(readingQuestions());
        when(wordAttemptLogRepository
                .existsByTrainingIdAndQuestionNoAndFinalAttemptTrueAndHasAudioDataTrueAndPronunciationAccuracyScoreIsNotNull(
                        30L,
                        1
                )).thenReturn(true);
        when(wordAttemptLogRepository
                .existsByTrainingIdAndQuestionNoAndFinalAttemptTrueAndHasAudioDataTrueAndPronunciationAccuracyScoreIsNotNull(
                        30L,
                        2
                )).thenReturn(true);
        when(gazeSessionRepository.existsByTrainingIdAndStatusAndDataIsNotNull(
                30L,
                GazeSessionStatus.COMPLETED
        )).thenReturn(true);

        assertThatCode(() -> service.validateCompletion(30L))
                .doesNotThrowAnyException();
    }

    private void generatedData(String json) {
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        when(data.getGeneratedData()).thenReturn(json);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
    }

    private String readingQuestions() {
        return """
                {
                  "questions":[
                    {
                      "questionNo":1,
                      "type":"SENTENCE_READING",
                      "requiredInputs":["VOICE","GAZE"]
                    },
                    {
                      "questionNo":2,
                      "type":"SENTENCE_READING",
                      "requiredInputs":["VOICE","GAZE"]
                    }
                  ]
                }
                """;
    }
}
