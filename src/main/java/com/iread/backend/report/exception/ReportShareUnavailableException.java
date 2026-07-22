package com.iread.backend.report.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReportShareUnavailableException extends RuntimeException {

    public ReportShareUnavailableException() {
        super("유효한 리포트 공유 링크가 아닙니다.");
    }
}
