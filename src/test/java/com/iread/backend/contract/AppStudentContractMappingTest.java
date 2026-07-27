package com.iread.backend.contract;

import com.iread.backend.mypage.app.controller.MypageController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppStudentContractMappingTest {
    @Autowired
    RequestMappingHandlerMapping handlerMapping;

    @Test
    void exposesGrowthAndCharacterContractRoutes() {
        Set<Route> actual = new HashSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    actual.add(new Route(method, path));
                }
            }
        }

        assertThat(actual).contains(
                new Route(RequestMethod.GET, "/api/app/mypage/character"),
                new Route(RequestMethod.GET, "/api/app/student/{studentId}/growth")
        );
    }

    @Test
    void characterLookupUsesStudentIdFromLearningTokenWithoutQueryParameter() throws Exception {
        var method = MypageController.class.getDeclaredMethod(
                "getCharacters",
                Long.class,
                Long.class
        );

        assertThat(method.getParameters())
                .noneMatch(parameter -> parameter.isAnnotationPresent(RequestParam.class));
    }

    private record Route(RequestMethod method, String path) {
    }
}
