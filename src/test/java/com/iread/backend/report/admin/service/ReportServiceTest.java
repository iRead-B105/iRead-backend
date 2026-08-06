package com.iread.backend.report.admin.service;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.ReportSnapshot;
import com.iread.backend.report.admin.exception.ReportCreationException;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.student.service.ReadingMetricAggregationService;
import com.iread.backend.student.service.ReadingMetricSummary;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.domain.TrainingEntity;
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

import java.math.BigDecimal;
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
    @Mock GazeAnalysisResultRepository gazeAnalysisResultRepository;
    @Mock GazeSessionRepository gazeSessionRepository;
    @Mock ReadingMetricAggregationService readingMetricAggregationService;
    @Mock com.iread.backend.training.app.service.DemoLearningClock demoLearningClock;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, studentRepository, trainingRepository,
                testRepository, wordAttemptLogRepository, gazeAnalysisResultRepository,
                gazeSessionRepository, readingMetricAggregationService, demoLearningClock,
                JsonMapper.builder().build());
        // 보고서 종료일 상한은 아동의 학습 날짜다. 테스트는 먼 미래로 두어
        // 기존 기간 검증만 확인한다.
        org.mockito.Mockito.lenient().when(demoLearningClock.currentDate(any()))
                .thenReturn(java.time.LocalDate.of(2999, 12, 31));
        org.mockito.Mockito.lenient().when(readingMetricAggregationService.summarize(
                any(), any(), any()
        )).thenReturn(new ReadingMetricSummary(
                "reading-metrics-v1",
                "PERCENT",
                "CORRECT_WORDS_PER_MINUTE",
                null,
                null,
                List.of()
        ));
    }

    @Test
    void 완료_학습이_있는_기간의_보고서를_저장한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        TrainingEntity training = org.mockito.Mockito.mock(TrainingEntity.class);
        TrainingEntity secondTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        when(training.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 0));
        when(training.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 5));
        when(secondTraining.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 0));
        when(secondTraining.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 5));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(training, secondTraining));
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(wordAttemptLogRepository.findIncorrectWordStats(any(), any(), any()))
                .thenReturn(List.of());
        when(readingMetricAggregationService.summarize(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(new ReadingMetricSummary(
                "reading-metrics-v1",
                "PERCENT",
                "CORRECT_WORDS_PER_MINUTE",
                new BigDecimal("70.00"),
                new BigDecimal("60.00"),
                List.of(
                        new ReadingMetricSummary.DailyMetric(
                                LocalDate.of(2026, 7, 10),
                                new BigDecimal("60.00"),
                                new BigDecimal("50.00")
                        ),
                        new ReadingMetricSummary.DailyMetric(
                                LocalDate.of(2026, 7, 11),
                                new BigDecimal("80.00"),
                                new BigDecimal("70.00")
                        )
                )
        ));
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReportEntity report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 25L);
            ReflectionTestUtils.setField(
                    report, "createdAt", LocalDateTime.of(2026, 7, 24, 12, 0)
            );
            return report;
        });

        var response = reportService.createReport(1L, new CreateReportRequest(
                10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertThat(response.reportId()).isEqualTo(25L);
        assertThat(response.createdAt()).isEqualTo(
                LocalDateTime.of(2026, 7, 24, 12, 0)
        );
        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSnapshotData())
                .contains("\"snapshotVersion\":\"teacher-report-v2\"")
                .contains("\"calculationVersion\":\"reading-metrics-v1\"")
                .contains("\"learningDays\":2")
                .contains("\"readingSpeedUnit\":\"CORRECT_WORDS_PER_MINUTE\"")
                .contains("\"growthComparisonStatus\":\"AVAILABLE\"")
                .contains("\"direction\":\"INCREASED\"")
                .contains("\"improvedPatterns\":[]");
        assertThat(captor.getValue().getTeacherMemo()).isNull();
    }

    @Test
    void 두_시점의_증가_감소_유지를_정확한_차이로_자동_분석한다() throws Exception {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        TrainingEntity firstTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        TrainingEntity latestTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        var firstTest = org.mockito.Mockito.mock(
                com.iread.backend.test.domain.StudentTestEntity.class
        );
        var latestTest = org.mockito.Mockito.mock(
                com.iread.backend.test.domain.StudentTestEntity.class
        );
        when(firstTraining.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 0));
        when(firstTraining.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 5));
        when(latestTraining.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 0));
        when(latestTraining.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 5));
        when(firstTest.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 12, 0));
        when(firstTest.getResult()).thenReturn("{\"pronunciationScore\":70}");
        when(latestTest.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 12, 0));
        when(latestTest.getResult()).thenReturn("{\"pronunciationScore\":80}");
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any()
        )).thenReturn(List.of(firstTraining, latestTraining));
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any()
        )).thenReturn(List.of(firstTest, latestTest));
        when(wordAttemptLogRepository.findIncorrectWordStats(any(), any(), any()))
                .thenReturn(List.of());
        when(readingMetricAggregationService.summarize(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(new ReadingMetricSummary(
                "reading-metrics-v1",
                "PERCENT",
                "CORRECT_WORDS_PER_MINUTE",
                new BigDecimal("80.00"),
                new BigDecimal("55.00"),
                List.of(
                        new ReadingMetricSummary.DailyMetric(
                                LocalDate.of(2026, 7, 10),
                                new BigDecimal("80.00"),
                                new BigDecimal("60.00")
                        ),
                        new ReadingMetricSummary.DailyMetric(
                                LocalDate.of(2026, 7, 11),
                                new BigDecimal("80.00"),
                                new BigDecimal("50.00")
                        )
                )
        ));
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reportService.createReport(1L, new CreateReportRequest(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        ));

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        ReportSnapshot stored = JsonMapper.builder().build().readValue(
                captor.getValue().getSnapshotData(),
                ReportSnapshot.class
        );
        assertThat(stored.automaticAnalysis().status())
                .isEqualTo(ReportSnapshot.AnalysisStatus.AVAILABLE);
        assertThat(stored.automaticAnalysis().metricChanges())
                .extracting(
                        ReportSnapshot.MetricChange::metric,
                        ReportSnapshot.MetricChange::delta,
                        ReportSnapshot.MetricChange::direction
                )
                .containsExactly(
                        tuple(
                                ReportSnapshot.MetricType.ACCURACY,
                                new BigDecimal("0.00"),
                                ReportSnapshot.ChangeDirection.UNCHANGED
                        ),
                        tuple(
                                ReportSnapshot.MetricType.READING_SPEED,
                                new BigDecimal("-10.00"),
                                ReportSnapshot.ChangeDirection.DECREASED
                        ),
                        tuple(
                                ReportSnapshot.MetricType.PRONUNCIATION_SCORE,
                                new BigDecimal("10.00"),
                                ReportSnapshot.ChangeDirection.INCREASED
                        )
                );
        assertThat(stored.automaticAnalysis().descriptions()).hasSize(3);
        assertThat(stored.improvedPatterns()).isEmpty();
        assertThat(stored.persistentDifficultyPatterns()).isEmpty();
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
                        LocalDate.of(2026, 7, 31)
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception -> {
            assertThat(exception.code()).isEqualTo("REPORT_INSUFFICIENT_LEARNING_DAYS");
            assertThat(exception.details()).containsEntry("requiredDays", 1)
                    .containsEntry("actualDays", 0L);
        });
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void sameDayTrainingsCountAsOneLearningDayAndCanCreateReport() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        TrainingEntity first = org.mockito.Mockito.mock(TrainingEntity.class);
        TrainingEntity second = org.mockito.Mockito.mock(TrainingEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(first.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 0));
        when(second.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 15, 0));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(first, second));
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(wordAttemptLogRepository.findIncorrectWordStats(any(), any(), any()))
                .thenReturn(List.of());
        when(readingMetricAggregationService.summarize(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(new ReadingMetricSummary(
                "reading-metrics-v1",
                "PERCENT",
                "CORRECT_WORDS_PER_MINUTE",
                BigDecimal.ZERO.setScale(2),
                null,
                List.of(new ReadingMetricSummary.DailyMetric(
                        LocalDate.of(2026, 7, 10),
                        BigDecimal.ZERO.setScale(2),
                        null
                ))
        ));
        when(reportRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reportService.createReport(
                1L,
                new CreateReportRequest(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)
                )
        );

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSnapshotData())
                .contains("\"learningDays\":1")
                .contains("\"averageAccuracy\":0.00")
                .contains("\"growthComparisonStatus\":\"INSUFFICIENT_DATA\"")
                .contains("비교할 기록이 부족합니다.");
    }

    @Test
    void completedTestsDoNotCountAsLearningDays() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        com.iread.backend.test.domain.StudentTestEntity completedTest =
                org.mockito.Mockito.mock(com.iread.backend.test.domain.StudentTestEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(completedTest));

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                new CreateReportRequest(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception -> {
            assertThat(exception.code()).isEqualTo("REPORT_INSUFFICIENT_LEARNING_DAYS");
            assertThat(exception.details()).containsEntry("actualDays", 0L);
        });
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
                        LocalDate.of(2026, 7, 31)
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
        TrainingEntity secondTraining = org.mockito.Mockito.mock(TrainingEntity.class);
        when(training.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 10, 10, 5));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(secondTraining.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 0));
        when(secondTraining.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 11, 10, 5));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of(training, secondTraining));
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
                        LocalDate.of(2026, 7, 31)
                )
        )).isInstanceOfSatisfying(ReportCreationException.class, exception ->
                assertThat(exception.code()).isEqualTo("REPORT_PERIOD_ALREADY_EXISTS"));
    }

    @Test
    void 시작일이_종료일보다_늦으면_리포트를_생성할_수_없다() {
        assertThatThrownBy(() -> reportService.createReport(1L, new CreateReportRequest(
                10L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1))))
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
        GazeAnalysisResultEntity first = gazeResult(
                301L, firstSession, LocalDateTime.of(2026, 7, 10, 10, 0),
                42000, 68, 7, 617
        );
        GazeAnalysisResultEntity latest = gazeResult(
                302L, latestSession, LocalDateTime.of(2026, 7, 20, 10, 0),
                35000, 58, 5, 603
        );
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L))
                .thenReturn(Optional.of(report));
        when(gazeAnalysisResultRepository
                .findAllByGazeSessionStudentIdAndGazeSessionContentTypeAndGazeSessionStartedAtGreaterThanEqualAndGazeSessionStartedAtLessThanOrderByCreatedAtAscIdAsc(
                        10L,
                        GazeContentType.TRAINING,
                        LocalDateTime.of(2026, 7, 1, 0, 0),
                        LocalDateTime.of(2026, 8, 1, 0, 0)
                ))
                .thenReturn(List.of(first, latest));

        var result = reportService.refreshGazeTrend(1L, 25L);
        reportService.refreshGazeTrend(1L, 25L);

        assertThat(result.reportId()).isEqualTo(25L);
        ReportSnapshot stored = JsonMapper.builder().build()
                .readValue(report.getSnapshotData(), ReportSnapshot.class);
        assertThat(stored.gazeTrend().training().points())
                .extracting(ReportSnapshot.GazePoint::gazeAnalysisResultId)
                .containsExactly(301L, 302L);
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
        return session;
    }

    private GazeAnalysisResultEntity gazeResult(
            Long id,
            GazeSessionEntity session,
            LocalDateTime createdAt,
            int totalVisitedDuration,
            int totalVisitedCount,
            int reverseReadCount,
            int avgVisitedDuration
    ) {
        GazeAnalysisResultEntity result = org.mockito.Mockito.mock(
                GazeAnalysisResultEntity.class
        );
        when(result.getId()).thenReturn(id);
        when(result.getGazeSession()).thenReturn(session);
        when(result.getCreatedAt()).thenReturn(createdAt);
        when(result.getTotalVisitedDuration()).thenReturn(totalVisitedDuration);
        when(result.getTotalVisitedCount()).thenReturn(totalVisitedCount);
        when(result.getReverseReadCount()).thenReturn(reverseReadCount);
        when(result.getAvgVisitedDuration()).thenReturn(avgVisitedDuration);
        return result;
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

    @Test
    void 종료일이_아동_학습_날짜보다_뒤면_거부한다() {
        // 데모 치트로 학습일을 넘기면 학습 날짜가 달력상 오늘보다 앞선다. 상한은
        // 달력이 아니라 그 학습 날짜여야 하고, 그보다 뒤는 여전히 막아야 한다.
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(demoLearningClock.currentDate(10L)).thenReturn(LocalDate.of(2026, 7, 20));

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                new CreateReportRequest(10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 21))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료일은 아동의 학습 날짜 이후일 수 없습니다.");
    }
}
