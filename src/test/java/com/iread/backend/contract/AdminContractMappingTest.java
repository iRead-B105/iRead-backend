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
class AdminContractMappingTest {
    @Autowired
    RequestMappingHandlerMapping handlerMapping;

    @Test
    void exposesAllReviewedTeacherAppContractRoutes() {
        Set<Route> actual = new HashSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    actual.add(new Route(method, path));
                }
            }
        }

        assertThat(actual).containsAll(expectedRoutes());
    }

    private Set<Route> expectedRoutes() {
        return Set.of(
                route(RequestMethod.POST, "/api/admin/report"),
                route(RequestMethod.GET, "/api/admin/report/{reportId}"),
                route(RequestMethod.POST, "/api/admin/report/{reportId}/gaze-analysis"),
                route(RequestMethod.PATCH, "/api/admin/report/{reportId}/teacher-memo"),

                route(RequestMethod.POST, "/api/admin/student"),
                route(RequestMethod.GET, "/api/admin/student/list"),
                route(RequestMethod.GET, "/api/admin/student/summary"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}"),
                route(RequestMethod.DELETE, "/api/admin/student/{studentId}"),
                route(RequestMethod.PATCH, "/api/admin/student/{studentId}"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}/accuracy-trend"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}/learning-events"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}/learning-summary"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}/training-history"),

                route(RequestMethod.GET, "/api/admin/teacher/info"),
                route(RequestMethod.PATCH, "/api/admin/teacher/profile"),

                route(RequestMethod.GET, "/api/admin/test/{studentId}/compare"),
                route(RequestMethod.GET, "/api/admin/test/{studentId}/list"),
                route(RequestMethod.GET, "/api/admin/test/{studentId}/{testId}/gaze-analysis"),

                route(RequestMethod.GET, "/api/admin/training/{studentId}"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/curriculum-log"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}"),
                route(RequestMethod.PATCH, "/api/admin/training/{studentId}/{curriculumId}"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}/statistics"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}/training-log"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{trainingId}/detail"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{trainingId}/expected-word"),
                route(RequestMethod.POST, "/api/admin/training/{studentId}/{trainingId}/expected-word"),
                route(RequestMethod.DELETE, "/api/admin/training/{studentId}/{trainingId}/expected-word/{wordId}"),
                route(RequestMethod.POST, "/api/admin/training/{studentId}/{trainingId}/export"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{trainingId}/gaze-analysis")
        );
    }

    private Route route(RequestMethod method, String path) {
        return new Route(method, path);
    }

    private record Route(RequestMethod method, String path) {
    }
}
