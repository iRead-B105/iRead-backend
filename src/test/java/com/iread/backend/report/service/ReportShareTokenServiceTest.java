package com.iread.backend.report.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportShareTokenServiceTest {

    private final ReportShareTokenService tokenService = new ReportShareTokenService();

    @Test
    void 원본_토큰과_SHA256_해시를_생성한다() {
        ReportShareToken token = tokenService.generate();

        assertThat(token.rawToken()).hasSize(43);
        assertThat(token.tokenHash()).hasSize(64);
        assertThat(token.tokenHash()).isNotEqualTo(token.rawToken());
        assertThat(tokenService.hash(token.rawToken())).isEqualTo(token.tokenHash());
    }

    @Test
    void 생성할_때마다_서로_다른_토큰을_반환한다() {
        ReportShareToken first = tokenService.generate();
        ReportShareToken second = tokenService.generate();

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(first.tokenHash()).isNotEqualTo(second.tokenHash());
    }
}
