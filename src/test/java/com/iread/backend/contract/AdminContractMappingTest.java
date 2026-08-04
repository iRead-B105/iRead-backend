package com.iread.backend.contract;

import com.iread.backend.student.controller.StudentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDate;
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

    @Test
    void requiresLearningEventTypeQueryParameter() throws Exception {
        var method = StudentController.class.getDeclaredMethod(
                "getLearningEvent",
                Long.class,
                Long.class,
                String.class,
                Long.class
        );
        RequestParam eventType = method.getParameters()[2].getAnnotation(RequestParam.class);

        assertThat(eventType).isNotNull();
        assertThat(eventType.name()).isEqualTo("eventType");
        assertThat(eventType.required()).isTrue();
    }

    @Test
    void exposesOptionalTrainingHistoryDateRangeQueryParameters() throws Exception {
        var method = StudentController.class.getDeclaredMethod(
                "getTrainingHistory",
                Long.class,
                Long.class,
                LocalDate.class,
                LocalDate.class
        );
        RequestParam from = method.getParameters()[2].getAnnotation(RequestParam.class);
        RequestParam to = method.getParameters()[3].getAnnotation(RequestParam.class);

        assertThat(from).isNotNull();
        assertThat(from.name()).isEqualTo("from");
        assertThat(from.required()).isFalse();
        assertThat(to).isNotNull();
        assertThat(to.name()).isEqualTo("to");
        assertThat(to.required()).isFalse();
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
                route(RequestMethod.GET, "/api/admin/student/{studentId}/reading-speed-trend"),
                route(RequestMethod.GET, "/api/admin/student/{studentId}/training-history"),

                route(RequestMethod.GET, "/api/admin/teacher/info"),
                route(RequestMethod.PATCH, "/api/admin/teacher/profile"),
                route(RequestMethod.PATCH, "/api/admin/teacher/profile/image"),

                route(RequestMethod.GET, "/api/admin/test/{studentId}/compare"),
                route(RequestMethod.GET, "/api/admin/test/{studentId}/list"),
                route(RequestMethod.GET, "/api/admin/test/{studentId}/{testId}/gaze-analysis"),

                route(RequestMethod.GET, "/api/admin/training/{studentId}"),
                route(RequestMethod.POST, "/api/admin/training/{studentId}/ai-recommendation"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/curriculum-log"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/current"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}"),
                route(RequestMethod.PATCH, "/api/admin/training/{studentId}/{curriculumId}"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}/statistics"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{curriculumId}/training-log"),
                route(RequestMethod.GET, "/api/admin/training/{studentId}/{trainingId}/detail"),
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
