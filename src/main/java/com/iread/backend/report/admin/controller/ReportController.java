package com.iread.backend.report.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.CreateReportResponse;
import com.iread.backend.report.admin.dto.res.ReportResponse;
import com.iread.backend.report.admin.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리포트 관리", description = "리포트 생성 및 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
public class ReportController {
    private final ReportService reportService;

    @Operation(summary = "리포트 상세 조회")
    @GetMapping("/{reportId}")
    public ReportResponse getReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportService.getReport(teacherId, reportId);
    }

    @Operation(summary = "리포트 생성")
    @PostMapping
    public CreateReportResponse createReport(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return reportService.createReport(teacherId, request);
    }
}
