package com.iread.backend.security;

import com.iread.backend.gaze.app.controller.GazeController;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.service.GazeService;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.student.app.controller.AppStudentController;
import com.iread.backend.student.app.service.GrowthService;
import com.iread.backend.student.app.service.AppStudentProfileService;
import com.iread.backend.story.app.controller.StoryController;
import com.iread.backend.story.app.service.StoryService;
import com.iread.backend.test.app.controller.AppTestController;
import com.iread.backend.test.app.service.AppTestService;
import com.iread.backend.training.app.controller.AppTrainingCatalogController;
import com.iread.backend.training.app.controller.AppTrainingController;
import com.iread.backend.training.app.service.AppTrainingCatalogService;
import com.iread.backend.training.app.service.AppTrainingService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AppResourceAuthorizationTest {

    private final StudentResourceAccessPolicy policy = new StudentResourceAccessPolicy();

    @Test
    void 성장조회에서다른학생경로접근을차단한다() {
        GrowthService service = mock(GrowthService.class);
        AppStudentController controller = new AppStudentController(
                service,
                mock(AppStudentProfileService.class),
                policy
        );

        assertThatThrownBy(() -> controller.getGrowth(1L, 20L, 21L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void 훈련조회에서다른학생경로접근을차단한다() {
        AppTrainingService service = mock(AppTrainingService.class);
        AppTrainingController controller = new AppTrainingController(service, policy);

        assertThatThrownBy(() -> controller.getIntro(1L, 20L, 21L, 30L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void 현재훈련목록에서다른학생경로접근을차단한다() {
        AppTrainingCatalogService service = mock(AppTrainingCatalogService.class);
        AppTrainingCatalogController controller =
                new AppTrainingCatalogController(service, policy);

        assertThatThrownBy(() -> controller.getCurrentTrainingList(1L, 20L, 21L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void 검사조회에서다른학생경로접근을차단한다() {
        AppTestService service = mock(AppTestService.class);
        AppTestController controller = new AppTestController(service, policy);

        assertThatThrownBy(() -> controller.getIntro(1L, 20L, 21L))
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
