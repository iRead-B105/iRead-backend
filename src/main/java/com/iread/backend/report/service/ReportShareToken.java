package com.iread.backend.report.service;

public record ReportShareToken(
        String rawToken,
        String tokenHash
) {
}
