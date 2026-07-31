package com.iread.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void allowsLearnerAppDevelopmentOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);
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
}
