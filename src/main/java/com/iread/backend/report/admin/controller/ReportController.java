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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "리포트 관리", description = "리포트 생성, 공유 및 피드백 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
public class ReportController {
    private final ReportService reportService;
    private final ReportShareService reportShareService;

    @Operation(summary = "리포트 상세 조회")
    @GetMapping("/{reportId}")
    public ReportResponse getReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportService.getReport(teacherId, reportId);
    }

    @Operation(summary = "리포트 생성")
    @PostMapping
    public CreateReportResponse createReport(@CurrentTeacherId Long teacherId,
                                               @Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(teacherId, request);
    }

    @Operation(summary = "리포트 외부 공유 링크 생성")
    @PostMapping("/{reportId}/shares")
    public CreateReportShareResponse createShare(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportShareService.createShare(teacherId, reportId);
    }

    @Operation(summary = "리포트 공유 이력 조회")
    @GetMapping("/{reportId}/shares")
    public List<ReportShareResponse> getShares(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportShareService.getShares(teacherId, reportId);
    }

    @Operation(summary = "외부 공유 리포트 피드백 목록 조회")
    @GetMapping("/feedbacks")
    public List<ReportFeedbackResponse> getFeedbacks(
            @CurrentTeacherId Long teacherId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return reportShareService.getFeedbacks(teacherId, unreadOnly);
    }

    @Operation(summary = "리포트 피드백 읽음 처리")
    @PatchMapping("/feedbacks/{feedbackId}/read")
    public ResponseEntity<Void> markFeedbackRead(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long feedbackId
    ) {
        reportShareService.markFeedbackRead(teacherId, feedbackId);
        return ResponseEntity.ok().build();
    }
}
