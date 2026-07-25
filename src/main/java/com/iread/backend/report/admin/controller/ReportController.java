package com.iread.backend.report.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.req.UpdateReportMemoRequest;
import com.iread.backend.report.admin.dto.res.CreateReportResponse;
import com.iread.backend.report.admin.dto.res.ReportListResponse;
import com.iread.backend.report.admin.dto.res.ReportResponse;
import com.iread.backend.report.admin.dto.res.UpdateReportMemoResponse;
import com.iread.backend.report.admin.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Tag(name = "리포트 관리", description = "리포트 생성 및 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
public class ReportController {
    private final ReportService reportService;

    @Operation(summary = "리포트 목록 조회")
    @GetMapping
    public List<ReportListResponse> getReports(
            @CurrentTeacherId Long teacherId,
            @RequestParam(required = false) Long studentId
    ) {
        return reportService.getReports(teacherId, studentId);
    }

    @Operation(summary = "리포트 상세 조회")
    @GetMapping("/{reportId}")
    public ReportResponse getReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportService.getReport(teacherId, reportId);
    }

    @Operation(summary = "리포트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateReportResponse createReport(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return reportService.createReport(teacherId, request);
    }

    @Operation(summary = "리포트 메모 수정")
    @PatchMapping("/{reportId}/teacher-memo")
    public UpdateReportMemoResponse updateReportMemo(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long reportId,
            @RequestBody UpdateReportMemoRequest request
    ) {
        return reportService.updateReportMemo(teacherId, reportId, request.teacherMemo());
    }

    @Operation(summary = "리포트 삭제")
    @DeleteMapping("/{reportId}")
    public void deleteReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        reportService.deleteReport(teacherId, reportId);
    }
}
