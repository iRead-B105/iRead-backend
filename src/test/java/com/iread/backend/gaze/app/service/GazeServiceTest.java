package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.analysis.GazeWordMetricMergeService;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GazeServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentTestRepository testRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock StoryRepository storyRepository;
    @Mock GazeSessionRepository gazeSessionRepository;
    @Mock GazeAnalysisResultRepository gazeAnalysisResultRepository;
    @Mock TrainingInputRequirementService trainingInputRequirementService;
    @Mock GazeWordMetricMergeService gazeWordMetricMergeService;
    @Mock RealtimeEventPublisher realtimeEventPublisher;

    @TempDir Path gazeStorageRoot;

    private GazeService gazeService;

    @BeforeEach
    void setUp() {
        gazeService = new GazeService(
                studentRepository,
                testRepository,
                trainingRepository,
                storyRepository,
                gazeSessionRepository,
                gazeAnalysisResultRepository,
                trainingInputRequirementService,
                gazeWordMetricMergeService,
                new GazeDataStorage(gazeStorageRoot.toString(), "/gaze"),
                realtimeEventPublisher,
                new JsonMapper()
        );
    }

    @Test
    void mapsLatestOwnedTestGazeAnalysisToAdminContractFields() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        GazeAnalysisResultEntity result = analysisResult();
        when(student.getId()).thenReturn(10L);
        when(test.getStudent()).thenReturn(student);
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(testRepository.findById(20L)).thenReturn(Optional.of(test));
        when(gazeAnalysisResultRepository
                .findFirstByGazeSessionStudentIdAndGazeSessionTestIdOrderByCreatedAtDesc(
                        10L, 20L
                ))
                .thenReturn(Optional.of(result));

        var response = gazeService.getTestGazeAnalysis(1L, 10L, 20L);

        assertThat(response.gazeSessionId()).isEqualTo(30L);
        assertThat(response.gazeAnalysisId()).isEqualTo(40L);
        assertThat(response.totalDwellTime()).isEqualTo(1200);
        assertThat(response.dwellCount()).isEqualTo(8);
        assertThat(response.regressionCount()).isEqualTo(2);
        assertThat(response.averageFixationTime()).isEqualTo(150);
    }

    @Test
    void mapsLatestOwnedTrainingGazeAnalysisToAdminContractFields() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        GazeAnalysisResultEntity result = analysisResult();
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(50L, 10L))
                .thenReturn(Optional.of(training));
        when(gazeAnalysisResultRepository
                .findFirstByGazeSessionStudentIdAndGazeSessionTrainingIdOrderByCreatedAtDesc(
                        10L, 50L
                ))
                .thenReturn(Optional.of(result));

        var response = gazeService.getTrainingGazeAnalysis(1L, 10L, 50L);

        assertThat(response.gazeAnalysisId()).isEqualTo(40L);
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
                .hasMessage("콘텐츠 식별자는 정확히 하나만 입력해야 합니다.");
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
                .hasMessage("contentType과 콘텐츠 식별자가 일치하지 않습니다.");
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
        doThrow(new ConflictException("이 훈련은 GAZE 입력을 사용하지 않습니다."))
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
                .hasMessageContaining("GAZE 입력을 사용하지 않습니다");

        verify(gazeSessionRepository, never()).saveAndFlush(
                org.mockito.ArgumentMatchers.any()
        );
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
                .hasMessageContaining("시선 샘플 또는 단어 지표");

        verify(gazeSessionRepository, never())
                .findByIdAndStudentIdForUpdate(30L, 10L);
    }

    @Test
    void storesRawGazeDataAsFileAndKeepsOnlyUrlOnSession() {
        StudentEntity student = mock(StudentEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(gazeSessionRepository.findByIdAndStudentIdForUpdate(30L, 10L))
                .thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.RUNNING);
        when(session.getId()).thenReturn(30L);

        gazeService.endSession(
                1L,
                30L,
                new EndGazeSessionRequest(
                        10L,
                        GazeSessionStatus.COMPLETED,
                        new JsonMapper().readTree("[{\"offsetMs\":120,\"x\":0.32,\"y\":0.41}]")
                )
        );

        ArgumentCaptor<String> dataUrl = ArgumentCaptor.forClass(String.class);
        verify(session).end(
                org.mockito.ArgumentMatchers.eq(GazeSessionStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(),
                dataUrl.capture()
        );
        assertThat(dataUrl.getValue())
                .matches("/gaze/10/gaze-30-[0-9a-f-]{36}\\.json");
        Path stored = gazeStorageRoot.resolve("10")
                .resolve(dataUrl.getValue().substring(dataUrl.getValue().lastIndexOf('/') + 1));
        assertThat(stored).exists();
        assertThat(stored).content(StandardCharsets.UTF_8)
                .isEqualTo("[{\"offsetMs\":120,\"x\":0.32,\"y\":0.41}]");
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
                    "text": "학교",
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
        when(session.getId()).thenReturn(30L);

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
                org.mockito.ArgumentMatchers.eq(GazeSessionStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.matches("/gaze/10/gaze-30-[0-9a-f-]{36}\\.json")
        );
        verify(gazeWordMetricMergeService).merge(session, data);
    }

    @Test
    void rejectsDuplicateAnalysisForCompletedSession() {
        StudentEntity student = mock(StudentEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(gazeSessionRepository.findByIdAndStudentIdForUpdate(30L, 10L)).thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.COMPLETED);
        when(gazeAnalysisResultRepository.existsByGazeSessionId(30L)).thenReturn(true);

        assertThatThrownBy(() -> gazeService.saveAnalysisResult(
                1L, 30L, new GazeAnalysisResultRequest(
                        10L, 1200, 8, 2, 150,
                        null, null, null, null
                )
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("시선 세션의 분석 결과가 이미 저장되어 있습니다.");

        verify(gazeAnalysisResultRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSentenceMetricsForNonStorySession() {
        StudentEntity student = mock(StudentEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(gazeSessionRepository.findByIdAndStudentIdForUpdate(30L, 10L)).thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.COMPLETED);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);

        assertThatThrownBy(() -> gazeService.saveAnalysisResult(
                1L, 30L,
                new GazeAnalysisResultRequest(
                        10L, 1200, 8, 2, 150,
                        List.of(new GazeAnalysisResultRequest.SentenceMetric(
                                1L, 1, "첫 문장", 1200, 8, 0, 1200
                        )),
                        null,
                        null,
                        null
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("문장별 시선 지표는 이야기 세션에서만 저장할 수 있습니다.");

        verify(gazeAnalysisResultRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private GazeAnalysisResultEntity analysisResult() {
        GazeAnalysisResultEntity result = mock(GazeAnalysisResultEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(result.getGazeSession()).thenReturn(session);
        when(session.getId()).thenReturn(30L);
        when(result.getId()).thenReturn(40L);
        when(result.getTotalVisitedDuration()).thenReturn(1200);
        when(result.getTotalVisitedCount()).thenReturn(8);
        when(result.getReverseReadCount()).thenReturn(2);
        when(result.getAvgVisitedDuration()).thenReturn(150);
        return result;
    }
}
