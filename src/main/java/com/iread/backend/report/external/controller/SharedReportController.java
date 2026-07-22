package com.iread.backend.report.external.controller;

import com.iread.backend.report.external.dto.req.SubmitReportFeedbackRequest;
import com.iread.backend.report.external.dto.res.SharedReportResponse;
import com.iread.backend.report.external.dto.res.SubmitReportFeedbackResponse;
import com.iread.backend.report.service.ReportShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/report")
public class SharedReportController {

    private final ReportShareService reportShareService;

    @GetMapping("/{shareToken}")
    public SharedReportResponse getSharedReport(@PathVariable String shareToken) {
        return reportShareService.getSharedReport(shareToken);
    }

    @PostMapping("/{shareToken}/feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitReportFeedbackResponse submitFeedback(
            @PathVariable String shareToken,
            @Valid @RequestBody SubmitReportFeedbackRequest request
    ) {
        return reportShareService.submitFeedback(shareToken, request.content());
    }
}
