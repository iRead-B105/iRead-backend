package com.iread.backend.report.admin.service;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.report.repository.StudentWordStatRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock StudentWordStatRepository wordStatRepository;
    @Mock GazeAnalysisResultRepository gazeAnalysisResultRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, studentRepository, trainingRepository,
                testRepository, wordStatRepository, gazeAnalysisResultRepository,
                JsonMapper.builder().build());
    }

    @Test
    void 데이터가_없는_기간도_빈_스냅샷으로_저장한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(wordStatRepository.findAllByStudentId(10L)).thenReturn(List.of());
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
                .contains("\"learningDays\":0")
                .contains("\"improvedPatterns\":[]");
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
    void 저장된_스냅샷을_계약_응답으로_평탄화한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        ReportEntity report = report(student, 25L, "메모");
        ReflectionTestUtils.setField(report, "snapshotData", snapshotJson());
        ReflectionTestUtils.setField(
                report, "createdAt", LocalDateTime.of(2026, 7, 24, 12, 0)
        );
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L))
                .thenReturn(Optional.of(report));

        var result = reportService.getReport(1L, 25L);

        assertThat(result.learningDays()).isEqualTo(3);
        assertThat(result.totalTrainingTime()).isEqualTo(45);
        assertThat(result.achievementByDomain()).containsEntry(
                "낱말 읽기", new java.math.BigDecimal("82.50")
        );
        assertThat(result.frequentErrorWords()).containsExactly("사과");
        assertThat(result.teacherMemo()).isEqualTo("메모");
    }

    @Test
    void 같은_아동의_시선_분석을_보고서_스냅샷에_반영한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(student.getId()).thenReturn(10L);
        ReportEntity report = report(student, 25L, null);
        ReflectionTestUtils.setField(report, "snapshotData", snapshotJson());
        GazeAnalysisResultEntity analysis = org.mockito.Mockito.mock(
                GazeAnalysisResultEntity.class
        );
        GazeSessionEntity session = org.mockito.Mockito.mock(GazeSessionEntity.class);
        when(analysis.getId()).thenReturn(50L);
        when(analysis.getGazeSession()).thenReturn(session);
        when(analysis.getTotalVisitedDuration()).thenReturn(1200);
        when(analysis.getTotalVisitedCount()).thenReturn(8);
        when(analysis.getReverseReadCount()).thenReturn(2);
        when(analysis.getAvgVisitedDuration()).thenReturn(150);
        when(session.getStudent()).thenReturn(student);
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L))
                .thenReturn(Optional.of(report));
        when(gazeAnalysisResultRepository.findByIdAndGazeSessionStudentTeacherId(50L, 1L))
                .thenReturn(Optional.of(analysis));

        var result = reportService.applyGazeAnalysis(1L, 25L, 50L);

        assertThat(result.reportId()).isEqualTo(25L);
        assertThat(report.getSnapshotData())
                .contains("\"gazeAnalysisResultId\":50")
                .contains("\"regressionCount\":2");
    }

    @Test
    void 소유한_리포트를_삭제한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        ReportEntity report = report(student, 25L, null);
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L)).thenReturn(Optional.of(report));

        reportService.deleteReport(1L, 25L);

        verify(reportRepository).delete(report);
    }

    @Test
    void 다른_교사의_리포트는_삭제할_수_없다() {
        when(reportRepository.findByIdAndStudentTeacherId(25L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.deleteReport(1L, 25L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(reportRepository, never()).delete(any());
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
