package com.iread.backend.contract;

import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.GazeSessionResponse;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.story.app.dto.res.StoryChoiceResponse;
import com.iread.backend.story.app.dto.res.StoryLineResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppStoryGazeContractTest {

    @Autowired RequestMappingHandlerMapping handlerMapping;
    @Autowired ObjectMapper objectMapper;

    @Test
    void exposesAllStoryAndGazeContractRoutes() {
        Set<Route> actual = new HashSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    actual.add(new Route(method, path));
                }
            }
        }

        assertThat(actual).contains(
                route(RequestMethod.GET, "/api/app/story/{studentId}"),
                route(RequestMethod.GET, "/api/app/story/{studentId}/{storyId}/lines"),
                route(RequestMethod.GET, "/api/app/story/{studentId}/{storyId}/lines/{lineId}"),
                route(RequestMethod.POST, "/api/app/story/{studentId}/{storyId}/lines/{lineId}/branches"),
                route(RequestMethod.GET, "/api/app/story/{studentId}/{storyId}/resume"),
                route(RequestMethod.POST, "/api/app/story/{studentId}/{storyId}/speech"),
                route(RequestMethod.POST, "/api/app/story/{studentId}/{storyId}/tts"),
                route(RequestMethod.GET, "/api/app/story/{studentId}/{storyTemplateId}"),
                route(RequestMethod.POST, "/api/app/story/{studentId}/{storyTemplateId}/sessions"),
                route(RequestMethod.GET, "/api/app/gaze/calibration-guide"),
                route(RequestMethod.GET, "/api/app/gaze/device/status"),
                route(RequestMethod.POST, "/api/app/gaze/sessions"),
                route(RequestMethod.POST, "/api/app/gaze/sessions/{gazeSessionId}/analysis-results"),
                route(RequestMethod.PATCH, "/api/app/gaze/sessions/{gazeSessionId}/end"),
                route(RequestMethod.PATCH, "/api/app/gaze/sessions/{gazeSessionId}/failed")
        );
    }

    @Test
    void serializesReviewedStoryAndGazeFieldNames() {
        StoryLineResponse line = new StoryLineResponse(
                1L, null, 2L, 3L, null, true, "어디로 갈까요?",
                1, 2, LocalDateTime.of(2026, 7, 28, 10, 0), null
        );
        StoryChoiceResponse choice = new StoryChoiceResponse(
                4L, "친구를 따라간다", 5L, 6L, "새 모험이 시작됐어요.",
                null, 100, "completed", false
        );
        GazeSessionResponse gaze = new GazeSessionResponse(
                7L, GazeContentType.STORY, GazeSessionStatus.RUNNING,
                GazeCalibrationStatus.SUCCESS, LocalDateTime.of(2026, 7, 28, 10, 0), null
        );

        var lineJson = objectMapper.valueToTree(line);
        var choiceJson = objectMapper.valueToTree(choice);
        var gazeJson = objectMapper.valueToTree(gaze);

        assertThat(lineJson.has("lineId")).isTrue();
        assertThat(lineJson.has("sceneId")).isTrue();
        assertThat(lineJson.has("lineText")).isTrue();
        assertThat(lineJson.has("sceneOrder")).isTrue();
        assertThat(lineJson.has("lineOrder")).isTrue();
        assertThat(choiceJson.has("choiceId")).isTrue();
        assertThat(choiceJson.has("replayed")).isTrue();
        assertThat(gazeJson.has("collectionStatus")).isTrue();
        assertThat(gazeJson.has("status")).isFalse();
    }

    @Test
    void acceptsEndStatusContractField() throws Exception {
        EndGazeSessionRequest request = objectMapper.readValue(
                """
                {"studentId":1,"endStatus":"COMPLETED","data":[{"x":10,"y":20}]}
                """,
                EndGazeSessionRequest.class
        );

        assertThat(request.endStatus()).isEqualTo(GazeSessionStatus.COMPLETED);
        assertThat(request.data()).hasSize(1);
    }

    private Route route(RequestMethod method, String path) {
        return new Route(method, path);
    }

    private record Route(RequestMethod method, String path) {
    }
}
