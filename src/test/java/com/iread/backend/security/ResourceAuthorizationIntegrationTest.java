package com.iread.backend.security;

import com.iread.backend.auth.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void learningTokenCannotSubscribeToTeacherRealtimeStream() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/admin/realtime/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenCannotSubscribeToLearnerRealtimeStream() throws Exception {
        String token = jwtTokenService.issueAdminAccessToken(1L).value();

        mockMvc.perform(get("/api/app/realtime/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedClientCannotSubscribeToRealtimeStream() throws Exception {
        mockMvc.perform(get("/api/app/realtime/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapTokenCanReachLinkedStudentProfileImageLookup() throws Exception {
        String token = jwtTokenService.issueBootstrapToken(1L).value();

        mockMvc.perform(get("/api/auth/app/students/999999/profile-image")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminTokenCannotReachLinkedStudentProfileImageLookup() throws Exception {
        String token = jwtTokenService.issueAdminAccessToken(1L).value();

        mockMvc.perform(get("/api/auth/app/students/999999/profile-image")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedClientCannotReadLinkedStudentProfileImage() throws Exception {
        mockMvc.perform(get("/api/auth/app/students/999999/profile-image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 학습토큰으로다른학생의성장정보에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/student/21/growth")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로다른학생의시선세션을시작하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(post("/api/app/gaze/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": 21,
                                  "contentType": "TEST",
                                  "testId": 30,
                                  "calibrationStatus": "SUCCESS"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로다른학생의이야기책장에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/story/21")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로다른학생의훈련에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/training/21/30/intro")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로다른학생의검사에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/test/21/intro")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로다른학생의생성음성에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/story/21/audio/tts-00000000-0000-0000-0000-000000000000.mp3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 제거된외부공유보고서경로는익명접근을거부한다() throws Exception {
        mockMvc.perform(get("/api/admin/report/shared/demo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자토큰으로학습App리소스에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueAdminAccessToken(1L).value();

        mockMvc.perform(get("/api/app/mypage/character")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학습토큰으로관리자리소스에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/admin/teacher/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
