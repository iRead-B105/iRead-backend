package com.iread.backend.report.service;

import com.iread.backend.report.admin.dto.res.ReportSnapshot;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.domain.ReportFeedbackEntity;
import com.iread.backend.report.domain.ReportShareEntity;
import com.iread.backend.report.exception.ReportShareUnavailableException;
import com.iread.backend.report.repository.ReportFeedbackRepository;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.report.repository.ReportShareRepository;
import com.iread.backend.student.domain.StudentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportShareServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock ReportShareRepository reportShareRepository;
    @Mock ReportFeedbackRepository reportFeedbackRepository;
    @Mock ReportShareTokenService tokenService;
    @Mock ObjectMapper objectMapper;

    private ReportShareService reportShareService;

    @BeforeEach
    void setUp() {
        reportShareService = new ReportShareService(
                reportRepository,
                reportShareRepository,
                reportFeedbackRepository,
                tokenService,
                objectMapper
        );
        ReflectionTestUtils.setField(
                reportShareService,
                "publicBaseUrl",
                "https://iread.example.com/shared/report/"
        );
    }

    @Test
    void 공유_링크를_생성하고_DB에는_토큰_해시와_30일_만료일을_저장한다() {
        ReportEntity report = mock(ReportEntity.class);
        when(reportRepository.findByIdAndStudentTeacherId(10L, 1L)).thenReturn(Optional.of(report));
        when(tokenService.generate()).thenReturn(new ReportShareToken("raw-token", "hashed-token"));
        when(reportShareRepository.saveAndFlush(any(ReportShareEntity.class))).thenAnswer(invocation -> {
            ReportShareEntity share = invocation.getArgument(0);
            ReflectionTestUtils.setField(share, "id", 100L);
            ReflectionTestUtils.setField(share, "createdAt", LocalDateTime.of(2026, 7, 22, 12, 0));
            return share;
        });
        LocalDateTime before = LocalDateTime.now().plusDays(30).minusSeconds(1);

        var response = reportShareService.createShare(1L, 10L);

        LocalDateTime after = LocalDateTime.now().plusDays(30).plusSeconds(1);
        assertThat(response.shareId()).isEqualTo(100L);
        assertThat(response.shareUrl()).isEqualTo("https://iread.example.com/shared/report/raw-token");
        assertThat(response.expiresAt()).isBetween(before, after);

        ArgumentCaptor<ReportShareEntity> captor = ArgumentCaptor.forClass(ReportShareEntity.class);
        verify(reportShareRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-token");
        assertThat(captor.getValue().getTokenHash()).doesNotContain("raw-token");
    }

    @Test
    void 유효한_공유_토큰으로_외부_리포트를_조회한다() throws Exception {
        StudentEntity student = mock(StudentEntity.class);
        ReportEntity report = report(student);
        ReportShareEntity share = share(report, LocalDateTime.now().plusDays(10));
        ReportSnapshot snapshot = mock(ReportSnapshot.class);
        when(tokenService.hash("raw-token")).thenReturn("hashed-token");
        when(reportShareRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(share));
        when(objectMapper.readValue("snapshot-json", ReportSnapshot.class)).thenReturn(snapshot);
        when(student.getName()).thenReturn("김학생");

        var response = reportShareService.getSharedReport("raw-token");

        assertThat(response.studentName()).isEqualTo("김학생");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.snapshot()).isSameAs(snapshot);
        assertThat(response.teacherMemo()).isEqualTo("꾸준히 성장하고 있습니다.");
    }

    @Test
    void 만료된_공유_토큰으로는_리포트를_조회할_수_없다() {
        ReportEntity report = mock(ReportEntity.class);
        ReportShareEntity share = share(report, LocalDateTime.now().minusSeconds(1));
        when(tokenService.hash("expired-token")).thenReturn("expired-hash");
        when(reportShareRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> reportShareService.getSharedReport("expired-token"))
                .isInstanceOf(ReportShareUnavailableException.class)
                .hasMessage("유효한 리포트 공유 링크가 아닙니다.");
    }

    @Test
    void 외부_사용자의_피드백을_공유_리포트에_저장한다() {
        ReportEntity report = mock(ReportEntity.class);
        ReportShareEntity share = share(report, LocalDateTime.now().plusDays(10));
        when(tokenService.hash("raw-token")).thenReturn("hashed-token");
        when(reportShareRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(share));
        when(reportFeedbackRepository.saveAndFlush(any(ReportFeedbackEntity.class))).thenAnswer(invocation -> {
            ReportFeedbackEntity feedback = invocation.getArgument(0);
            ReflectionTestUtils.setField(feedback, "id", 200L);
            ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 7, 22, 13, 0));
            return feedback;
        });

        var response = reportShareService.submitFeedback("raw-token", "  가정에서도 연습하겠습니다.  ");

        assertThat(response.feedbackId()).isEqualTo(200L);
        ArgumentCaptor<ReportFeedbackEntity> captor = ArgumentCaptor.forClass(ReportFeedbackEntity.class);
        verify(reportFeedbackRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getReportShare()).isSameAs(share);
        assertThat(captor.getValue().getContent()).isEqualTo("가정에서도 연습하겠습니다.");
    }

    @Test
    void 교사는_본인_학생의_미확인_피드백을_조회하고_읽음_처리한다() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(20L);
        when(student.getName()).thenReturn("김학생");
        ReportEntity report = report(student);
        ReflectionTestUtils.setField(report, "id", 10L);
        ReportShareEntity share = share(report, LocalDateTime.now().plusDays(10));
        ReportFeedbackEntity feedback = new ReportFeedbackEntity(share, "확인했습니다.");
        ReflectionTestUtils.setField(feedback, "id", 200L);
        ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 7, 22, 13, 0));
        when(reportFeedbackRepository
                .findAllByReportShareReportStudentTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(feedback));
        when(reportFeedbackRepository.findByIdAndReportShareReportStudentTeacherId(200L, 1L))
                .thenReturn(Optional.of(feedback));

        var responses = reportShareService.getFeedbacks(1L, true);
        reportShareService.markFeedbackRead(1L, 200L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().studentName()).isEqualTo("김학생");
        assertThat(feedback.getReadAt()).isNotNull();
        verify(reportFeedbackRepository, never())
                .findAllByReportShareReportStudentTeacherIdOrderByCreatedAtDesc(any());
    }

    private ReportEntity report(StudentEntity student) {
        ReportEntity report = mock(ReportEntity.class);
        when(report.getStudent()).thenReturn(student);
        lenient().when(report.getStartDate()).thenReturn(LocalDate.of(2026, 7, 1));
        lenient().when(report.getEndDate()).thenReturn(LocalDate.of(2026, 7, 31));
        lenient().when(report.getSnapshotData()).thenReturn("snapshot-json");
        lenient().when(report.getTeacherMemo()).thenReturn("꾸준히 성장하고 있습니다.");
        return report;
    }

    private ReportShareEntity share(ReportEntity report, LocalDateTime expiresAt) {
        ReportShareEntity share = new ReportShareEntity(report, "hashed-token", expiresAt);
        ReflectionTestUtils.setField(share, "id", 100L);
        ReflectionTestUtils.setField(share, "createdAt", LocalDateTime.of(2026, 7, 22, 12, 0));
        return share;
    }
}
