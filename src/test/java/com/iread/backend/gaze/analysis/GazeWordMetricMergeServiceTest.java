package com.iread.backend.gaze.analysis;

import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GazeWordMetricMergeServiceTest {

    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock TrainingInputRequirementService trainingInputRequirementService;
    @Mock TestDataRepository testDataRepository;

    private GazeWordMetricMergeService service;
    private JsonMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JsonMapper();
        service = new GazeWordMetricMergeService(
                wordAttemptLogRepository,
                trainingInputRequirementService,
                testDataRepository,
                new WordAttemptScoreCalculator(
                        new WordAttemptScoreProperties(
                                100,
                                70,
                                200,
                                600,
                                100,
                                50,
                                30,
                                20
                        )
                ),
                objectMapper
        );
    }

    @Test
    void mergesEyetrackerWordMetricsAndPreservesRetryPenalty() {
        TrainingEntity training = mock(TrainingEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);
        when(session.getTraining()).thenReturn(training);
        when(training.getId()).thenReturn(30L);
        when(trainingInputRequirementService.inputsForQuestion(30L, 1))
                .thenReturn(Set.of(
                        TrainingInputType.VOICE,
                        TrainingInputType.GAZE
                ));

        WordAttemptLogEntity attempt = new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                training,
                "학교",
                true,
                null,
                null,
                null,
                null,
                false,
                0,
                800,
                0,
                500,
                true,
                null,
                1,
                0,
                1,
                true
        );
        WordAttemptLogEntity previousAttempt = new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                training,
                "학교",
                true,
                null,
                null,
                null,
                null,
                false,
                0,
                700,
                0,
                500,
                false,
                null,
                1,
                0,
                1,
                false
        );
        when(wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndTargetIndex(30L, 1, 0))
                .thenReturn(List.of(previousAttempt, attempt));

        service.merge(session, objectMapper.readTree("""
                {
                  "schemaVersion": 1,
                  "words": [{
                    "questionNo": 1,
                    "targetIndex": 0,
                    "tokenIndex": 1,
                    "index": 1,
                    "text": "학교",
                    "dwellMs": 650,
                    "visitCount": 2,
                    "regressionCount": 1,
                    "firstSeenMs": 100,
                    "lastSeenMs": 750
                  }]
                }
                """));

        assertThat(attempt.getFixationDurationMs()).isEqualTo(650);
        assertThat(attempt.getFixationCount()).isEqualTo(2);
        assertThat(attempt.getGazeStartOffsetMs()).isEqualTo(100);
        assertThat(attempt.getGazeEndOffsetMs()).isEqualTo(750);
        assertThat(attempt.getRegressionCount()).isEqualTo(1);
        assertThat(attempt.getSkipped()).isFalse();
        assertThat(attempt.getTotalScore()).isEqualTo(850);
    }

    @Test
    void rejectsTrainingWordMetricWithoutQuestionPosition() {
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);

        assertThatThrownBy(() -> service.merge(
                session,
                objectMapper.readTree("""
                        {
                          "words": [{
                            "index": 0,
                            "text": "학교",
                            "dwellMs": 650,
                            "visitCount": 2,
                            "regressionCount": 0
                          }]
                        }
                        """)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("questionNo");
    }

    @Test
    void keepsReadWordUnskippedWhenFixationThresholdIsNotMet() throws Exception {
        TrainingEntity training = mock(TrainingEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);
        when(session.getTraining()).thenReturn(training);
        when(training.getId()).thenReturn(30L);
        when(trainingInputRequirementService.inputsForQuestion(30L, 1))
                .thenReturn(Set.of(
                        TrainingInputType.VOICE,
                        TrainingInputType.GAZE
                ));
        WordAttemptLogEntity attempt = new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                training,
                "beta",
                true,
                null,
                null,
                null,
                null,
                false,
                0,
                900,
                0,
                500,
                true,
                null,
                1,
                0,
                1,
                true
        );
        when(wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndTargetIndex(30L, 1, 0))
                .thenReturn(List.of(attempt));

        service.merge(session, objectMapper.readTree("""
                {
                  "words": [{
                    "questionNo": 1,
                    "targetIndex": 0,
                    "tokenIndex": 1,
                    "text": "beta",
                    "dwellMs": 0,
                    "visitCount": 0,
                    "readCount": 1,
                    "skipped": false,
                    "regressionCount": 0,
                    "firstSeenMs": 1000,
                    "lastSeenMs": 2200
                  }]
                }
                """));

        assertThat(attempt.getSkipped()).isFalse();
        assertThat(attempt.getFixationDurationMs()).isZero();
        assertThat(attempt.getFixationCount()).isZero();
        assertThat(attempt.getTotalScore()).isEqualTo(950);
    }

    @Test
    void marksUnvisitedWordAsSkippedAndAppliesZeroGazeComponent() {
        TrainingEntity training = mock(TrainingEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);
        when(session.getTraining()).thenReturn(training);
        when(training.getId()).thenReturn(30L);
        when(trainingInputRequirementService.inputsForQuestion(30L, 1))
                .thenReturn(Set.of(
                        TrainingInputType.VOICE,
                        TrainingInputType.GAZE
                ));
        WordAttemptLogEntity attempt = new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                training,
                "학교",
                true,
                null,
                null,
                null,
                null,
                false,
                0,
                900,
                0,
                500,
                true,
                null,
                1,
                0,
                0,
                true
        );
        when(wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndTargetIndex(30L, 1, 0))
                .thenReturn(List.of(attempt));

        service.merge(session, objectMapper.readTree("""
                {
                  "words": [{
                    "questionNo": 1,
                    "targetIndex": 0,
                    "tokenIndex": 0,
                    "index": 0,
                    "text": "학교",
                    "dwellMs": 0,
                    "visitCount": 0,
                    "regressionCount": 0,
                    "firstSeenMs": null,
                    "lastSeenMs": null
                  }]
                }
                """));

        assertThat(attempt.getSkipped()).isTrue();
        assertThat(attempt.getTotalScore()).isEqualTo(530);
    }

    @Test
    void mergesZeroBasedGazeTokenWithSingleTargetAttempt() throws Exception {
        TrainingEntity training = mock(TrainingEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);
        when(session.getTraining()).thenReturn(training);
        when(training.getId()).thenReturn(30L);
        when(trainingInputRequirementService.inputsForQuestion(30L, 1))
                .thenReturn(Set.of(
                        TrainingInputType.VOICE,
                        TrainingInputType.GAZE
                ));
        WordAttemptLogEntity attempt = new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(WordEntity.class),
                training,
                "ㅏ",
                true,
                null,
                null,
                null,
                null,
                false,
                0,
                950,
                0,
                400,
                true,
                null,
                1,
                0,
                null,
                true
        );
        when(wordAttemptLogRepository
                .findAllByTrainingIdAndQuestionNoAndTargetIndex(30L, 1, 0))
                .thenReturn(List.of(attempt));

        service.merge(session, objectMapper.readTree("""
                {
                  "words": [{
                    "questionNo": 1,
                    "targetIndex": 0,
                    "tokenIndex": 0,
                    "text": "ㅏ",
                    "dwellMs": 420,
                    "visitCount": 1,
                    "regressionCount": 0,
                    "firstSeenMs": 0,
                    "lastSeenMs": 420
                  }]
                }
                """));

        assertThat(attempt.getFixationDurationMs()).isEqualTo(420);
        assertThat(attempt.getFixationCount()).isEqualTo(1);
        assertThat(attempt.getTotalScore()).isEqualTo(975);
    }
}
