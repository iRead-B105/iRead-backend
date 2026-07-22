package com.iread.backend.report.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.CreateReportResponse;
import com.iread.backend.report.admin.dto.res.CreateReportShareResponse;
import com.iread.backend.report.admin.dto.res.ReportFeedbackResponse;
import com.iread.backend.report.admin.dto.res.ReportResponse;
import com.iread.backend.report.admin.dto.res.ReportShareResponse;
import com.iread.backend.report.admin.service.ReportService;
import com.iread.backend.report.service.ReportShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
public class ReportController {
    private final ReportService reportService;
    private final ReportShareService reportShareService;

    @GetMapping("/{reportId}")
    public ReportResponse getReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportService.getReport(teacherId, reportId);
    }

    @PostMapping
    public CreateReportResponse createReport(@CurrentTeacherId Long teacherId,
                                               @Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(teacherId, request);
    }

    @PostMapping("/{reportId}/shares")
    public CreateReportShareResponse createShare(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportShareService.createShare(teacherId, reportId);
    }

    @GetMapping("/{reportId}/shares")
    public List<ReportShareResponse> getShares(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportShareService.getShares(teacherId, reportId);
    }

    @GetMapping("/feedbacks")
    public List<ReportFeedbackResponse> getFeedbacks(
            @CurrentTeacherId Long teacherId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return reportShareService.getFeedbacks(teacherId, unreadOnly);
    }

    @PatchMapping("/feedbacks/{feedbackId}/read")
    public ResponseEntity<Void> markFeedbackRead(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long feedbackId
    ) {
        reportShareService.markFeedbackRead(teacherId, feedbackId);
        return ResponseEntity.ok().build();
    }
}
