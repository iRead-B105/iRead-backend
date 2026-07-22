package com.iread.backend.report.service;

import com.iread.backend.report.admin.dto.res.*;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.domain.ReportFeedbackEntity;
import com.iread.backend.report.domain.ReportShareEntity;
import com.iread.backend.report.exception.ReportShareUnavailableException;
import com.iread.backend.report.external.dto.res.SharedReportResponse;
import com.iread.backend.report.external.dto.res.SubmitReportFeedbackResponse;
import com.iread.backend.report.repository.ReportFeedbackRepository;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.report.repository.ReportShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportShareService {

    private static final int SHARE_VALID_DAYS = 30;

    private final ReportRepository reportRepository;
    private final ReportShareRepository reportShareRepository;
    private final ReportFeedbackRepository reportFeedbackRepository;
    private final ReportShareTokenService tokenService;
    private final ObjectMapper objectMapper;

    @Value("${app.report-share.public-base-url:http://localhost:5173/shared/report}")
    private String publicBaseUrl;

    @Transactional
    public CreateReportShareResponse createShare(Long teacherId, Long reportId) {
        ReportEntity report = findOwnedReport(teacherId, reportId);
        ReportShareToken token = tokenService.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(SHARE_VALID_DAYS);
        ReportShareEntity share = reportShareRepository.saveAndFlush(
                new ReportShareEntity(report, token.tokenHash(), expiresAt)
        );

        return new CreateReportShareResponse(
                share.getId(),
                buildShareUrl(token.rawToken()),
                share.getExpiresAt(),
                share.getCreatedAt()
        );
    }

    public List<ReportShareResponse> getShares(Long teacherId, Long reportId) {
        findOwnedReport(teacherId, reportId);
        LocalDateTime now = LocalDateTime.now();
        return reportShareRepository.findAllByReportIdOrderByCreatedAtDesc(reportId).stream()
                .map(share -> new ReportShareResponse(
                        share.getId(),
                        reportId,
                        share.getExpiresAt(),
                        share.getCreatedAt(),
                        share.isExpired(now)
                ))
                .toList();
    }

    public SharedReportResponse getSharedReport(String rawToken) {
        ReportEntity report = findActiveShare(rawToken).getReport();
        return new SharedReportResponse(
                report.getStudent().getName(),
                report.getStartDate(),
                report.getEndDate(),
                readSnapshot(report.getSnapshotData()),
                report.getTeacherMemo()
        );
    }

    @Transactional
    public SubmitReportFeedbackResponse submitFeedback(String rawToken, String content) {
        ReportShareEntity share = findActiveShare(rawToken);
        ReportFeedbackEntity feedback = reportFeedbackRepository.saveAndFlush(
                new ReportFeedbackEntity(share, content.trim())
        );
        return new SubmitReportFeedbackResponse(feedback.getId(), feedback.getCreatedAt());
    }

    public List<ReportFeedbackResponse> getFeedbacks(Long teacherId, boolean unreadOnly) {
        List<ReportFeedbackEntity> feedbacks = unreadOnly
                ? reportFeedbackRepository
                        .findAllByReportShareReportStudentTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(teacherId)
                : reportFeedbackRepository
                        .findAllByReportShareReportStudentTeacherIdOrderByCreatedAtDesc(teacherId);
        return feedbacks.stream().map(this::toFeedbackResponse).toList();
    }

    @Transactional
    public void markFeedbackRead(Long teacherId, Long feedbackId) {
        ReportFeedbackEntity feedback = reportFeedbackRepository
                .findByIdAndReportShareReportStudentTeacherId(feedbackId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));
        feedback.markRead(LocalDateTime.now());
    }

    private ReportEntity findOwnedReport(Long teacherId, Long reportId) {
        return reportRepository.findByIdAndStudentTeacherId(reportId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
    }

    private ReportShareEntity findActiveShare(String rawToken) {
        String tokenHash = tokenService.hash(rawToken);
        ReportShareEntity share = reportShareRepository.findByTokenHash(tokenHash)
                .orElseThrow(ReportShareUnavailableException::new);
        if (share.isExpired(LocalDateTime.now())) {
            throw new ReportShareUnavailableException();
        }
        return share;
    }

    private String buildShareUrl(String rawToken) {
        return publicBaseUrl.replaceAll("/+$", "") + "/" + rawToken;
    }

    private ReportFeedbackResponse toFeedbackResponse(ReportFeedbackEntity feedback) {
        ReportEntity report = feedback.getReportShare().getReport();
        return new ReportFeedbackResponse(
                feedback.getId(),
                report.getId(),
                report.getStudent().getId(),
                report.getStudent().getName(),
                feedback.getContent(),
                feedback.getCreatedAt(),
                feedback.getReadAt()
        );
    }

    private ReportSnapshot readSnapshot(String snapshotData) {
        try {
            return objectMapper.readValue(snapshotData, ReportSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalStateException("리포트 스냅샷 조회에 실패했습니다.", exception);
        }
    }
}
