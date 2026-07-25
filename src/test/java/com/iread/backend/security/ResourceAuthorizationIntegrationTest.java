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
    void 학습토큰으로다른학생의마이페이지에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueLearningAccessToken(1L, 20L).value();

        mockMvc.perform(get("/api/app/mypage/character")
                        .queryParam("studentId", "21")
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
    void 제거된외부공유보고서경로는익명접근을거부한다() throws Exception {
        mockMvc.perform(get("/api/admin/report/shared/demo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자토큰으로학습App리소스에접근하면403을반환한다() throws Exception {
        String token = jwtTokenService.issueAdminAccessToken(1L).value();

        mockMvc.perform(get("/api/app/mypage/character")
                        .queryParam("studentId", "20")
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
