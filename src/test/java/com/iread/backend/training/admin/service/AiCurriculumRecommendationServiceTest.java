package com.iread.backend.training.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiCurriculumRecommendationServiceTest {

    private static final UUID REQUEST_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String REQUEST_ID =
            "curriculum-recommendation-7-" + REQUEST_UUID;

    private StudentRepository studentRepository;
    private StudentFeatureProfileService profileService;
    private TrainingTemplateRepository templateRepository;
    private MockRestServiceServer server;
    private AiCurriculumRecommendationService service;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        profileService = mock(StudentFeatureProfileService.class);
        templateRepository = mock(TrainingTemplateRepository.class);
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-server");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AiCurriculumRecommendationService(
                studentRepository,
                profileService,
                templateRepository,
                builder.build(),
                () -> REQUEST_UUID
        );
    }

    @Test
    void recommendsFiveExistingTemplatesFromSpecificStudentProfiles() {
        when(studentRepository.findByIdAndTeacherId(7L, 3L))
                .thenReturn(Optional.of(mock(StudentEntity.class)));
        when(profileService.getProfiles(7L)).thenReturn(List.of(
                profile("GRAPHEME.VOWEL", 0.61, 0.50),
                profile("GRAPHEME.CODA.COMPLEX.ㄺ", 0.42, 0.73)
        ));

        List<TrainingTemplateEntity> templates = new ArrayList<>();
        for (long id = 15; id < 20; id++) {
            TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
            when(template.getId()).thenReturn(id);
            when(template.getName()).thenReturn("훈련 " + id);
            templates.add(template);
        }
        when(templateRepository.findAllById(any())).thenReturn(templates);

        server.expect(requestTo("http://ai-server/api/v1/curricula/recommend"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Idempotency-Key", REQUEST_ID))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.featureProfiles.length()").value(1))
                .andExpect(jsonPath("$.featureProfiles[0].featureCode")
                        .value("GRAPHEME.CODA.COMPLEX.ㄺ"))
                .andExpect(jsonPath("$.useLlm").value(true))
                .andRespond(withSuccess(aiResponse(), MediaType.APPLICATION_JSON));

        var response = service.recommend(3L, 7L);

        assertThat(response.recommendationProvider()).isEqualTo("openai");
        assertThat(response.currentStage()).isEqualTo(3);
        assertThat(response.recommendations())
                .extracting(item -> item.trainingTemplateId())
                .containsExactly(15L, 16L, 17L, 18L, 19L);
        assertThat(response.recommendations().getFirst().trainingName()).isEqualTo("훈련 15");
        server.verify();
    }

    @Test
    void rejectsAStudentOutsideTheCurrentTeacherBeforeCallingAi() {
        when(studentRepository.findByIdAndTeacherId(7L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommend(3L, 7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("학생을 찾을 수 없습니다.");
        verify(profileService, never()).getProfiles(any());
    }

    private StudentFeatureProfileService.StudentFeatureProfileView profile(
            String featureCode,
            double accuracy,
            double weakness
    ) {
        return new StudentFeatureProfileService.StudentFeatureProfileView(
                featureCode,
                accuracy,
                54.0,
                1_350,
                BigDecimal.valueOf(3.1),
                BigDecimal.valueOf(2.4),
                0.18,
                2_800,
                weakness,
                0.91,
                20,
                StudentFeatureProfileService.ProfileStatus.WEAK,
                StudentFeatureProfileService.ANALYSIS_VERSION,
                LocalDateTime.parse("2026-08-04T12:00:00")
        );
    }

    private String aiResponse() {
        return """
                {
                  "requestId": "%s",
                  "schemaVersion": 1,
                  "recommendationProvider": "openai",
                  "dataSufficiency": "SUFFICIENT",
                  "currentStage": 3,
                  "maximumAllowedStage": 4,
                  "stageRationale": "3단계 보완 훈련을 추천합니다.",
                  "recommendations": [
                    %s
                  ],
                  "candidateAudit": [],
                  "warnings": []
                }
                """.formatted(REQUEST_ID, recommendationItems());
    }

    private String recommendationItems() {
        List<String> items = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            int templateId = 15 + index;
            String role = index < 3 ? "CORE" : index == 3 ? "REINFORCEMENT" : "STRETCH";
            items.add("""
                    {
                      "sequenceNo": %d,
                      "trainingTemplateId": %d,
                      "trainingName": "AI 응답 이름",
                      "role": "%s",
                      "curriculumStage": 3,
                      "recommendedDifficulty": 2,
                      "score": 0.75,
                      "targetFeatureCodes": ["GRAPHEME.CODA.COMPLEX.ㄺ"],
                      "reasonCodes": ["HIGH_WEAKNESS"],
                      "rationale": "겹받침을 보완합니다."
                    }
                    """.formatted(index + 1, templateId, role));
        }
        return String.join(",", items);
    }
}
