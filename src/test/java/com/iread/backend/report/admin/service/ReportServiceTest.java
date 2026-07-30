package com.iread.backend.report.admin.service;

import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.ReportSnapshot;
import com.iread.backend.report.admin.exception.ReportCreationException;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reportRepository;
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock StudentTestRepository testRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock GazeSessionRepository gazeSessionRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, studentRepository, trainingRepository,
                testRepository, wordAttemptLogRepository, gazeSessionRepository,
                JsonMapper.builder().build());
    }

    @Test
    void 완료_학습이_있는_기간의_보고서를_저장한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        TrainingEntity training = org.mockito.Mockito.mock(TrainingEntity.class);
        when(training.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 0));
        when(training.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 5));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(training));
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(wordAttemptLogRepository.findIncorrectWordStats(any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReportEntity report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 25L);
            ReflectionTestUtils.setField(
                    report, "createdAt", LocalDateTime.of(2026, 7, 24, 12, 0)
            );
            return report;
        });

        var response = reportService.createReport(1L, new CreateReportRequest(
                10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "메모"));

        assertThat(response.reportId()).isEqualTo(25L);
        assertThat(response.createdAt()).isEqualTo(
                LocalDateTime.of(2026, 7, 24, 12, 0)
        );
        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSnapshotData())
                .contains("\"learningDays\":1")
                .contains("\"improvedPatterns\":[]");
    }

    @Test
    void 완료_학습이_없는_기간은_보고서를_저장하지_않는다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                new CreateReportRequest(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception ->
                assertThat(exception.code()).isEqualTo("REPORT_DATA_NOT_FOUND"));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동일_아동과_기간의_보고서는_기존_식별자와_함께_차단한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        ReportEntity existing = report(student, 25L, null);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(reportRepository.findByStudentIdAndStartDateAndEndDate(
                10L,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 23, 59, 59, 999_999_999)
        )).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                new CreateReportRequest(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception -> {
            assertThat(exception.code()).isEqualTo("REPORT_PERIOD_ALREADY_EXISTS");
            assertThat(exception.details()).containsEntry("existingReportId", 25L);
        });
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시_중복_저장은_기간_중복_오류로_변환한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        TrainingEntity training = org.mockito.Mockito.mock(TrainingEntity.class);
        when(training.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 0));
        when(training.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 5));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(training));
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(wordAttemptLogRepository.findIncorrectWordStats(any(), any(), any()))
                .thenReturn(List.of());
        when(reportRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                new CreateReportRequest(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception ->
                assertThat(exception.code()).isEqualTo("REPORT_PERIOD_ALREADY_EXISTS"));
    }

    @Test
    void 시작일이_종료일보다_늦으면_리포트를_생성할_수_없다() {
        assertThatThrownBy(() -> reportService.createReport(1L, new CreateReportRequest(
                10L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void 학생별_리포트_목록을_최신순으로_반환한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(student.getId()).thenReturn(10L);
        when(student.getName()).thenReturn("학생");
        ReportEntity report = report(student, 25L, "기존 메모");
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(reportRepository.findAllByStudentIdAndStudentTeacherIdOrderByCreatedAtDesc(10L, 1L))
                .thenReturn(List.of(report));

        var response = reportService.getReports(1L, 10L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().reportId()).isEqualTo(25L);
        assertThat(response.getFirst().studentName()).isEqualTo("학생");
        assertThat(response.getFirst().teacherMemo()).isEqualTo("기존 메모");
    }

    @Test
    void 소유한_리포트의_메모를_수정한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        ReportEntity report = report(student, 25L, "기존 메모");
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L)).thenReturn(Optional.of(report));

        reportService.updateReportMemo(1L, 25L, "  수정 메모  ");

        assertThat(report.getTeacherMemo()).isEqualTo("수정 메모");
    }

    @Test
    void 저장된_스냅샷을_중첩_계약으로_반환한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(student.getId()).thenReturn(10L);
        ReportEntity report = report(student, 25L, "메모");
        ReflectionTestUtils.setField(report, "snapshotData", snapshotJson());
        ReflectionTestUtils.setField(
                report, "createdAt", LocalDateTime.of(2026, 7, 24, 12, 0)
        );
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L))
                .thenReturn(Optional.of(report));

        var result = reportService.getReport(1L, 25L);

        assertThat(result.studentId()).isEqualTo(10L);
        assertThat(result.snapshot().learningDays()).isEqualTo(3);
        assertThat(result.snapshot().totalTrainingTimeMinutes()).isEqualTo(45);
        assertThat(result.snapshot().areaAchievements())
                .extracting("area", "achievement")
                .containsExactly(tuple("낱말 읽기", new java.math.BigDecimal("82.50")));
        assertThat(result.snapshot().frequentlyIncorrectWords())
                .extracting("wordName")
                .containsExactly("사과");
        assertThat(result.teacherMemo()).isEqualTo("메모");
    }

    @Test
    void 보고서_기간의_훈련_시선_분석을_추이로_갱신한다() throws Exception {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(student.getId()).thenReturn(10L);
        ReportEntity report = report(student, 25L, null);
        ReflectionTestUtils.setField(report, "snapshotData", snapshotJson());
        TrainingEntity firstTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        TrainingEntity latestTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        when(firstTraining.getId()).thenReturn(101L);
        when(latestTraining.getId()).thenReturn(102L);
        GazeSessionEntity firstSession = gazeSession(201L, firstTraining);
        GazeSessionEntity latestSession = gazeSession(202L, latestTraining);
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L))
                .thenReturn(Optional.of(report));
        when(gazeSessionRepository
                .findAllByStudentIdAndContentTypeAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAscIdAsc(
                        10L,
                        GazeContentType.TRAINING,
                        GazeSessionStatus.COMPLETED,
                        LocalDateTime.of(2026, 7, 1, 0, 0),
                        LocalDateTime.of(2026, 8, 1, 0, 0)
                ))
                .thenReturn(List.of(firstSession, latestSession));
        when(wordAttemptLogRepository.findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(101L))
                .thenReturn(List.of(attempt(42000, 68, 7)));
        when(wordAttemptLogRepository.findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(102L))
                .thenReturn(List.of(attempt(35000, 58, 5)));

        var result = reportService.refreshGazeTrend(1L, 25L);
        reportService.refreshGazeTrend(1L, 25L);

        assertThat(result.reportId()).isEqualTo(25L);
        ReportSnapshot stored = JsonMapper.builder().build()
                .readValue(report.getSnapshotData(), ReportSnapshot.class);
        assertThat(stored.gazeTrend().training().points())
                .extracting(ReportSnapshot.GazePoint::gazeAnalysisResultId)
                .containsExactly(null, null);
        assertThat(stored.gazeTrend().training().changes().reverseReadCount())
                .isEqualTo(new ReportSnapshot.GazeMetricChange(7, 5, -2));
        assertThat(stored.gazeTrend().training().points()).hasSize(2);
        assertThat(stored.gazeTrend().test().status())
                .isEqualTo(ReportSnapshot.GazeSeriesStatus.NO_DATA);
    }

    private ReportEntity report(StudentEntity student, Long id, String memo) {
        ReportEntity report = new ReportEntity(
                student,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "{}",
                memo
        );
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private GazeSessionEntity gazeSession(Long id, TrainingEntity training) {
        GazeSessionEntity session = org.mockito.Mockito.mock(GazeSessionEntity.class);
        when(session.getId()).thenReturn(id);
        when(session.getTraining()).thenReturn(training);
        when(session.getEndedAt()).thenReturn(LocalDateTime.of(2026, 7, 20, 10, 0));
        return session;
    }

    private WordAttemptLogEntity attempt(
            int fixationDurationMs,
            int fixationCount,
            int regressionCount
    ) {
        return new WordAttemptLogEntity(
                org.mockito.Mockito.mock(StudentEntity.class),
                org.mockito.Mockito.mock(com.iread.backend.training.domain.WordEntity.class),
                null,
                "test",
                true,
                fixationDurationMs,
                fixationCount,
                null,
                null,
                false,
                regressionCount,
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

    private String snapshotJson() {
        return """
                {
                  "learningDays": 3,
                  "totalTrainingTimeMinutes": 45,
                  "completedTrainingCount": 5,
                  "averageAccuracy": 80.00,
                  "averageReadingSpeed": 72.00,
                  "readingSpeedUnit": "CPM",
                  "growthHistory": [],
                  "areaAchievements": [{
                    "area": "낱말 읽기",
                    "achievement": 82.50
                  }],
                  "frequentlyIncorrectWords": [{
                    "wordId": 1,
                    "wordName": "사과",
                    "attemptCount": 5,
                    "incorrectCount": 2,
                    "incorrectRate": 40.00
                  }],
                  "improvedPatterns": [],
                  "persistentDifficultyPatterns": [],
                  "gazeAnalysis": null
                }
                """;
    }
}
