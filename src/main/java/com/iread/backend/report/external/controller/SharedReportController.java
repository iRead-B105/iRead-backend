package com.iread.backend.report.external.controller;

import com.iread.backend.report.external.dto.req.SubmitReportFeedbackRequest;
import com.iread.backend.report.external.dto.res.SharedReportResponse;
import com.iread.backend.report.external.dto.res.SubmitReportFeedbackResponse;
import com.iread.backend.report.service.ReportShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공유 리포트", description = "외부 공유 리포트 조회 및 피드백 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report/shared")
public class SharedReportController {

    private final ReportShareService reportShareService;

    @Operation(summary = "공유 토큰으로 리포트 조회")
    @GetMapping("/{shareToken}")
    public SharedReportResponse getSharedReport(@PathVariable String shareToken) {
        return reportShareService.getSharedReport(shareToken);
    }

    @Operation(summary = "공유 리포트에 피드백 작성")
    @PostMapping("/{shareToken}/feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitReportFeedbackResponse submitFeedback(
            @PathVariable String shareToken,
            @Valid @RequestBody SubmitReportFeedbackRequest request
    ) {
        return reportShareService.submitFeedback(shareToken, request.content());
    }
}
