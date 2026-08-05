package com.iread.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    private static final List<String> DEVELOPMENT_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5174",
            "http://localhost:4173",
            "http://127.0.0.1:4173"
    );

    @Test
    void allowsLearnerAppDevelopmentOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(null, null, DEVELOPMENT_ORIGINS);
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration corsConfiguration =
                source.getCorsConfiguration(new MockHttpServletRequest(
                        "OPTIONS",
                        "/api/auth/app/teacher-login"
                ));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins()).contains(
                "http://localhost:5174",
                "http://127.0.0.1:5174"
        );
    }

    // 운영에서는 CORS_ALLOWED_ORIGINS 로 도메인을 주입한다. 이 목록에 운영 도메인이 없으면
    // 브라우저가 같은 출처 POST 에 붙이는 Origin 헤더 때문에 로그인이 403 으로 막힌다.
    @Test
    void allowsInjectedProductionOrigin() {
        SecurityConfig securityConfig = new SecurityConfig(
                null,
                null,
                List.of("https://i15b105.p.ssafy.io")
        );
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        CorsConfiguration corsConfiguration =
                source.getCorsConfiguration(new MockHttpServletRequest(
                        "OPTIONS",
                        "/api/auth/admin/login"
                ));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins())
                .containsExactly("https://i15b105.p.ssafy.io");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }
}
