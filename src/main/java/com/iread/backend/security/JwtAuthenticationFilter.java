package com.iread.backend.security;

import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.repository.AuthRefreshSessionRepository;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.JwtTokenService;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final AuthRefreshSessionRepository sessionRepository;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            AuthRefreshSessionRepository sessionRepository
    ) {
        this.jwtTokenService = jwtTokenService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthPrincipal principal = jwtTokenService.parseAndValidate(
                        authorization.substring(BEARER_PREFIX.length())
                );
                requireActiveSession(principal);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                authorities.add(new SimpleGrantedAuthority("AUD_" + principal.audience()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 단일 세션 정책: 접근 토큰이 묶인 로그인 세션(sid)이 밀려났으면 즉시 거부한다.
     * 부트스트랩 토큰(아동 선택용, 5분)은 세션이 없어 검사에서 제외한다.
     * sid 없는 구버전 접근 토큰도 거부해 재로그인하게 한다.
     */
    private void requireActiveSession(AuthPrincipal principal) {
        if (JwtTokenService.BOOTSTRAP_AUDIENCE.equals(principal.audience())) {
            return;
        }
        if (principal.sessionId() == null
                || !sessionRepository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(
                        principal.sessionId(),
                        Instant.now()
                )) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_REVOKED",
                    "다른 기기에서 로그인되어 이 세션은 종료되었습니다."
            );
        }
    }
}
