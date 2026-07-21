package com.iread.backend.report.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.CreateReportResponse;
import com.iread.backend.report.admin.dto.res.ReportResponse;
import com.iread.backend.report.admin.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/{reportId}")
    public ReportResponse getReport(@CurrentTeacherId Long teacherId, @PathVariable Long reportId) {
        return reportService.getReport(teacherId, reportId);
    }

    @PostMapping
    public CreateReportResponse createReport(@CurrentTeacherId Long teacherId,
                                               @Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(teacherId, request);
    }
}
