package com.iread.backend.report.admin.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ReportCreationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    private ReportCreationException(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details,
            Throwable cause
    ) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public static ReportCreationException insufficientLearningDays(long actualDays) {
        return new ReportCreationException(
                HttpStatus.BAD_REQUEST,
                "REPORT_INSUFFICIENT_LEARNING_DAYS",
                "At least two distinct completed training days are required.",
                Map.of(
                        "requiredDays", 2,
                        "actualDays", actualDays
                ),
                null
        );
    }

    public static ReportCreationException periodAlreadyExists(Long existingReportId) {
        Map<String, Object> details = existingReportId == null
                ? null
                : Map.of("existingReportId", existingReportId);
        return new ReportCreationException(
                HttpStatus.CONFLICT,
                "REPORT_PERIOD_ALREADY_EXISTS",
                "같은 기간의 보고서가 이미 있습니다.",
                details,
                null
        );
    }

    public static ReportCreationException periodAlreadyExists(Throwable cause) {
        return new ReportCreationException(
                HttpStatus.CONFLICT,
                "REPORT_PERIOD_ALREADY_EXISTS",
                "같은 기간의 보고서가 이미 있습니다.",
                null,
                cause
        );
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
