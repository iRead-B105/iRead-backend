package com.iread.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentResourceAccessPolicyTest {

    private final StudentResourceAccessPolicy policy = new StudentResourceAccessPolicy();

    @Test
    void 토큰의학생과요청학생이같으면허용한다() {
        assertThatCode(() -> policy.requireSameStudent(20L, 20L))
                .doesNotThrowAnyException();
    }

    @Test
    void 다른학생리소스접근을거부한다() {
        assertThatThrownBy(() -> policy.requireSameStudent(20L, 21L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 학생식별자가없는인증주체를거부한다() {
        assertThatThrownBy(() -> policy.requireSameStudent(null, 20L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
