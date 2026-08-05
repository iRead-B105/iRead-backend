package com.iread.backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityErrorHandler securityErrorHandler,
            // application.properties 를 읽지 않는 슬라이스 테스트 컨텍스트에서도 빈이 만들어지도록
            // 기본값을 둔다. 운영 값은 application.properties 의 CORS_ALLOWED_ORIGINS 로 주입된다.
            @Value("${app.cors.allowed-origins:"
                    + "http://localhost:5173,http://127.0.0.1:5173,"
                    + "http://localhost:5174,http://127.0.0.1:5174,"
                    + "http://localhost:4173,http://127.0.0.1:4173}")
            List<String> allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.csrf(csrf -> csrf.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
        );

        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                .requestMatchers(
                        "/",
                        "/api/auth/admin/login",
                        "/api/auth/admin/password-reset/request",
                        "/api/auth/admin/password-reset/confirm",
                        "/api/auth/admin/refresh",
                        "/api/auth/admin/sign-up",
                        "/api/auth/app/refresh",
                        "/api/auth/app/teacher-login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/error"
                ).permitAll()
                // <img> 태그는 Authorization 헤더를 보낼 수 없고 JwtAuthenticationFilter는
                // 헤더만 읽으므로 업로드 이미지가 401이 된다. 조회만 열어 두고 쓰기는 막는다.
                // 시연용 서버 한정 설정이다. 실제 아동 사진을 다루게 되면 되돌려야 한다.
                .requestMatchers(HttpMethod.GET, "/uploads/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/app/students/*/profile-image")
                .hasAnyAuthority("AUD_learning-bootstrap", "AUD_learning-app")
                .requestMatchers("/api/auth/app/student-login").hasAuthority("AUD_learning-bootstrap")
                .requestMatchers("/api/auth/admin/logout", "/api/admin/**").hasAuthority("AUD_admin-app")
                .requestMatchers("/api/auth/app/logout").hasAnyRole("TEACHER", "STUDENT")
                .requestMatchers("/api/app/tts").hasRole("STUDENT")
                .requestMatchers("/api/app/**").hasAuthority("AUD_learning-app")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Accept",
                "Last-Event-ID"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
