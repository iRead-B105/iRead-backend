package com.iread.backend.report.admin.service;

import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.report.repository.StudentWordStatRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.repository.StudentStudyProgressRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reportRepository;
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock StudentTestRepository testRepository;
    @Mock StudentStudyProgressRepository progressRepository;
    @Mock StudentWordStatRepository wordStatRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, studentRepository, trainingRepository,
                testRepository, progressRepository, wordStatRepository, JsonMapper.builder().build());
    }

    @Test
    void 데이터가_없는_기간도_빈_스냅샷으로_저장한다() {
        StudentEntity student = org.mockito.Mockito.mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(testRepository.findAllByStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any())).thenReturn(List.of());
        when(progressRepository.findAllByStudentId(10L)).thenReturn(List.of());
        when(wordStatRepository.findAllByStudentId(10L)).thenReturn(List.of());
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            ReportEntity report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 25L);
            return report;
        });

        var response = reportService.createReport(1L, new CreateReportRequest(
                10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "메모"));

        assertThat(response.reportId()).isEqualTo(25L);
        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).save(captor.capture());
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
}
