package com.iread.backend.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthContractMappingTest {

    @Autowired
    RequestMappingHandlerMapping handlerMapping;

    @Test
    void exposesReviewedAuthRoutesWithoutLegacyRecoveryEndpoints() {
        Set<Route> actual = new HashSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    actual.add(new Route(method, path));
                }
            }
        }

        assertThat(actual).containsAll(Set.of(
                route("/api/auth/admin/login"),
                route("/api/auth/admin/logout"),
                route("/api/auth/admin/password-reset/request"),
                route("/api/auth/admin/password-reset/confirm"),
                route("/api/auth/admin/refresh"),
                route("/api/auth/admin/sign-up"),
                route("/api/auth/app/logout"),
                route("/api/auth/app/refresh"),
                route("/api/auth/app/student-login"),
                route("/api/auth/app/teacher-login"),
                new Route(RequestMethod.GET, "/api/auth/app/students/{studentId}/profile-image")
        ));
        assertThat(actual).doesNotContain(
                route("/api/auth/admin/find-id"),
                route("/api/auth/admin/password-reset")
        );
    }

    private Route route(String path) {
        return new Route(RequestMethod.POST, path);
    }

    private record Route(RequestMethod method, String path) {
    }
}
