package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.analysis.GazeWordMetricMergeService;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GazeServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentTestRepository testRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock StoryRepository storyRepository;
    @Mock GazeSessionRepository gazeSessionRepository;
    @Mock TrainingInputRequirementService trainingInputRequirementService;
    @Mock GazeWordMetricMergeService gazeWordMetricMergeService;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;

    private GazeService gazeService;

    @BeforeEach
    void setUp() {
        gazeService = new GazeService(
                studentRepository,
                testRepository,
                trainingRepository,
                storyRepository,
                gazeSessionRepository,
                trainingInputRequirementService,
                gazeWordMetricMergeService,
                wordAttemptLogRepository
        );
    }

    @Test
    void mapsLatestOwnedTestGazeAnalysisFromWordAttemptLogs() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(student.getId()).thenReturn(10L);
        when(test.getStudent()).thenReturn(student);
        when(session.getId()).thenReturn(30L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(testRepository.findById(20L)).thenReturn(Optional.of(test));
        when(gazeSessionRepository.findFirstByStudentIdAndTestIdAndStatusOrderByEndedAtDescIdDesc(
                10L, 20L, GazeSessionStatus.COMPLETED
        )).thenReturn(Optional.of(session));
        when(wordAttemptLogRepository.findAllByTestIdAndFinalAttemptTrueOrderByIdAsc(20L))
                .thenReturn(List.of(attempt(1000, 5, 1), attempt(200, 3, 1)));

        var response = gazeService.getTestGazeAnalysis(1L, 10L, 20L);

        assertThat(response.gazeSessionId()).isEqualTo(30L);
        assertThat(response.gazeAnalysisId()).isNull();
        assertThat(response.totalDwellTime()).isEqualTo(1200);
        assertThat(response.dwellCount()).isEqualTo(8);
        assertThat(response.regressionCount()).isEqualTo(2);
        assertThat(response.averageFixationTime()).isEqualTo(150);
    }

    @Test
    void mapsLatestOwnedTrainingGazeAnalysisFromWordAttemptLogs() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(session.getId()).thenReturn(30L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(50L, 10L))
                .thenReturn(Optional.of(training));
        when(gazeSessionRepository.findFirstByStudentIdAndTrainingIdAndStatusOrderByEndedAtDescIdDesc(
                10L, 50L, GazeSessionStatus.COMPLETED
        )).thenReturn(Optional.of(session));
        when(wordAttemptLogRepository.findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(50L))
                .thenReturn(List.of(attempt(1200, 8, 2)));

        var response = gazeService.getTrainingGazeAnalysis(1L, 10L, 50L);

        assertThat(response.gazeAnalysisId()).isNull();
        assertThat(response.regressionCount()).isEqualTo(2);
    }

    @Test
    void rejectsMultipleContentReferences() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        StartGazeSessionRequest request = new StartGazeSessionRequest(
                10L, GazeContentType.TEST, 20L, 30L, null, GazeCalibrationStatus.SUCCESS
        );

        assertThatThrownBy(() -> gazeService.startSession(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly one content reference is required.");
    }

    @Test
    void rejectsReferenceThatDoesNotMatchContentType() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        StartGazeSessionRequest request = new StartGazeSessionRequest(
                10L, GazeContentType.TEST, null, 30L, null, GazeCalibrationStatus.SUCCESS
        );

        assertThatThrownBy(() -> gazeService.startSession(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contentType does not match the provided reference id.");
    }

    @Test
    void rejectsTrainingGazeSessionWhenQuestionDoesNotRequireGaze() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(50L, 10L))
                .thenReturn(Optional.of(training));
        when(training.getId()).thenReturn(50L);
        doThrow(new ConflictException("GAZE is not required."))
                .when(trainingInputRequirementService)
                .requireTrainingInput(50L, TrainingInputType.GAZE);
        StartGazeSessionRequest request = new StartGazeSessionRequest(
                10L,
                GazeContentType.TRAINING,
                null,
                50L,
                null,
                GazeCalibrationStatus.SUCCESS
        );

        assertThatThrownBy(() -> gazeService.startSession(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("GAZE");

        verify(gazeSessionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCompletedGazeSessionWithoutRawInput() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> gazeService.endSession(
                1L,
                30L,
                new EndGazeSessionRequest(10L, GazeSessionStatus.COMPLETED, null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sample or word gaze data");

        verify(gazeSessionRepository, never())
                .findByIdAndStudentIdForUpdate(30L, 10L);
    }

    @Test
    void acceptsStructuredWordMetricsAndMergesThemAfterSessionEnd() {
        StudentEntity student = mock(StudentEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        var data = new JsonMapper().readTree("""
                {
                  "schemaVersion": 1,
                  "words": [{
                    "questionNo": 1,
                    "targetIndex": 0,
                    "tokenIndex": 0,
                    "index": 0,
                    "text": "test",
                    "dwellMs": 500,
                    "visitCount": 1,
                    "regressionCount": 0
                  }]
                }
                """);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(gazeSessionRepository.findByIdAndStudentIdForUpdate(30L, 10L))
                .thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.RUNNING);

        gazeService.endSession(
                1L,
                30L,
                new EndGazeSessionRequest(
                        10L,
                        GazeSessionStatus.COMPLETED,
                        data
                )
        );

        verify(session).end(
                eq(GazeSessionStatus.COMPLETED),
                any(),
                eq(data.toString())
        );
        verify(gazeWordMetricMergeService).merge(session, data);
    }

    private WordAttemptLogEntity attempt(Integer duration, Integer count, Integer regressions) {
        return new WordAttemptLogEntity(
                mock(StudentEntity.class),
                mock(com.iread.backend.training.domain.WordEntity.class),
                null,
                "test",
                true,
                duration,
                count,
                null,
                null,
                false,
                regressions,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                0,
                true
        );
    }
}
