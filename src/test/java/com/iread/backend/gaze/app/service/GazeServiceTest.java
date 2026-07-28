package com.iread.backend.gaze.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock GazeAnalysisResultRepository gazeAnalysisResultRepository;

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
    void rejectsDuplicateAnalysisForCompletedSession() {
        StudentEntity student = mock(StudentEntity.class);
        GazeSessionEntity session = mock(GazeSessionEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(gazeSessionRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.COMPLETED);
        when(gazeAnalysisResultRepository.existsByGazeSessionId(30L)).thenReturn(true);

        assertThatThrownBy(() -> gazeService.saveAnalysisResult(
                1L, 30L, new GazeAnalysisResultRequest(10L, 1200, 8, 2, 150, null)
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
        when(gazeSessionRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(session));
        when(session.getStatus()).thenReturn(GazeSessionStatus.COMPLETED);
        when(session.getContentType()).thenReturn(GazeContentType.TRAINING);

        assertThatThrownBy(() -> gazeService.saveAnalysisResult(
                1L, 30L,
                new GazeAnalysisResultRequest(
                        10L, 1200, 8, 2, 150,
                        new JsonMapper().readTree("[{\"sentenceNo\":1}]")
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
