package com.iread.backend.security;

import com.iread.backend.gaze.app.controller.GazeController;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.service.GazeService;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.mypage.app.controller.MypageController;
import com.iread.backend.mypage.app.service.MypageService;
import com.iread.backend.story.app.controller.StoryController;
import com.iread.backend.story.app.service.StoryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AppResourceAuthorizationTest {

    private final StudentResourceAccessPolicy policy = new StudentResourceAccessPolicy();

    @Test
    void 마이페이지에서다른학생캐릭터접근을차단한다() {
        MypageService service = mock(MypageService.class);
        MypageController controller = new MypageController(service, policy);

        assertThatThrownBy(() -> controller.getCharacters(1L, 20L, 21L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void 이야기에서다른학생경로접근을차단한다() {
        StoryService service = mock(StoryService.class);
        StoryController controller = new StoryController(service, policy);

        assertThatThrownBy(() -> controller.getStoryShelf(1L, 20L, 21L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void 시선세션에서다른학생요청본문을차단한다() {
        GazeService service = mock(GazeService.class);
        GazeController controller = new GazeController(service, policy);
        StartGazeSessionRequest request = new StartGazeSessionRequest(
                21L,
                GazeContentType.TEST,
                30L,
                null,
                null,
                GazeCalibrationStatus.SUCCESS
        );

        assertThatThrownBy(() -> controller.startSession(1L, 20L, request))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }
}
