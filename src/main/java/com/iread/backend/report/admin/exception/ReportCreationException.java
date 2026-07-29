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

    public static ReportCreationException dataNotFound() {
        return new ReportCreationException(
                HttpStatus.BAD_REQUEST,
                "REPORT_DATA_NOT_FOUND",
                "선택한 기간에 완료된 학습 기록이 없습니다.",
                null,
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
