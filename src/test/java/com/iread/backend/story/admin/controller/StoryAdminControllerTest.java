package com.iread.backend.story.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.story.admin.dto.res.StoryPageEditResponse;
import com.iread.backend.story.admin.service.StoryAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoryAdminControllerTest {

    @Mock
    private StoryAdminService storyAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver teacherIdResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(CurrentTeacherId.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    org.springframework.web.bind.support.WebDataBinderFactory binderFactory
            ) {
                return 1L;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new StoryAdminController(storyAdminService))
                .setCustomArgumentResolvers(teacherIdResolver)
                .build();
    }

    @Test
    void bindsMultipartRevisionAsAFormField() throws Exception {
        when(storyAdminService.uploadUnreadPageImage(
                eq(1L), eq(10L), eq(30L), eq(50L), eq(7L), any()
        )).thenReturn(new StoryPageEditResponse(
                50L, 8L, null, "본문", List.of(), "/uploads/images/new.png", true
        ));
        MockMultipartFile image = new MockMultipartFile(
                "image", "scene.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart(
                        "/api/admin/student/10/story-history/30/pages/50/image"
                )
                .file(image)
                .param("revision", "7"))
                .andExpect(status().isOk());

        verify(storyAdminService).uploadUnreadPageImage(
                eq(1L), eq(10L), eq(30L), eq(50L), eq(7L), any()
        );
    }
}
