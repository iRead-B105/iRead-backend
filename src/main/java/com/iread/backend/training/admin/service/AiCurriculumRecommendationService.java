package com.iread.backend.training.admin.service;

import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.dto.res.AiCurriculumRecommendationResponse;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AiCurriculumRecommendationService {

    private static final String RECOMMEND_PATH = "/api/v1/curricula/recommend";
    private static final int RECOMMENDATION_COUNT = 5;
    private static final Set<String> AGGREGATE_FEATURE_CODES = Set.of(
            "GRAPHEME",
            "GRAPHEME.ONSET",
            "GRAPHEME.VOWEL",
            "GRAPHEME.CODA",
            "SYLLABLE",
            "PHONOLOGY",
            "WORD",
            "SENTENCE"
    );

    private final StudentRepository studentRepository;
    private final StudentFeatureProfileService profileService;
    private final TrainingTemplateRepository templateRepository;
    private final RestClient aiRestClient;
    private final Supplier<UUID> requestIdSupplier;

    @Autowired
    public AiCurriculumRecommendationService(
            StudentRepository studentRepository,
            StudentFeatureProfileService profileService,
            TrainingTemplateRepository templateRepository,
            @Qualifier("aiRestClient") RestClient aiRestClient
    ) {
        this(
                studentRepository,
                profileService,
                templateRepository,
                aiRestClient,
                UUID::randomUUID
        );
    }

    AiCurriculumRecommendationService(
            StudentRepository studentRepository,
            StudentFeatureProfileService profileService,
            TrainingTemplateRepository templateRepository,
            RestClient aiRestClient,
            Supplier<UUID> requestIdSupplier
    ) {
        this.studentRepository = studentRepository;
        this.profileService = profileService;
        this.templateRepository = templateRepository;
        this.aiRestClient = aiRestClient;
        this.requestIdSupplier = requestIdSupplier;
    }

    public AiCurriculumRecommendationResponse recommend(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        String requestId = "curriculum-recommendation-" + studentId + "-" + requestIdSupplier.get();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("requestId", requestId);
        request.put("schemaVersion", 1);
        request.put(
                "featureProfiles",
                profileService.getProfiles(studentId).stream()
                        .filter(profile -> isSpecificFeatureCode(profile.featureCode()))
                        .map(this::toAiProfile)
                        .toList()
        );
        request.put("recentTrainings", List.of());
        request.put("useLlm", true);

        JsonNode response = callAi(requestId, request);
        return validateAndMap(requestId, response);
    }

    private Map<String, Object> toAiProfile(
            StudentFeatureProfileService.StudentFeatureProfileView profile
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("featureCode", profile.featureCode());
        result.put("accuracyRate", profile.accuracyRate());
        putIfNonNull(result, "avgPronunciationScore", profile.avgPronunciationScore());
        putIfNonNull(result, "avgFixationDurationMs", profile.avgFixationDurationMs());
        putIfNonNull(result, "avgFixationCount", profile.avgFixationCount());
        putIfNonNull(result, "avgRegressionCount", profile.avgRegressionCount());
        result.put("skipRate", profile.skipRate());
        putIfNonNull(result, "avgReadingTimeMs", profile.avgReadingTimeMs());
        result.put("weaknessScore", profile.weaknessScore());
        result.put("confidence", profile.confidence());
        result.put("evidenceCount", profile.evidenceCount());
        return result;
    }

    private boolean isSpecificFeatureCode(String featureCode) {
        if (featureCode == null || featureCode.isBlank()) {
            return false;
        }
        return !AGGREGATE_FEATURE_CODES.contains(featureCode.toUpperCase());
    }

    private void putIfNonNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private JsonNode callAi(String requestId, Map<String, Object> request) {
        try {
            return aiRestClient.post()
                    .uri(RECOMMEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", requestId)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 커리큘럼 추천 서버가 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(JsonNode.class);
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 커리큘럼 추천 서버와 통신하지 못했습니다.", exception);
        }
    }

    private AiCurriculumRecommendationResponse validateAndMap(
            String requestId,
            JsonNode response
    ) {
        if (!requestId.equals(response.path("requestId").asText())) {
            throw new AiClientException("AI 커리큘럼 추천 응답의 requestId가 일치하지 않습니다.");
        }
        JsonNode items = response.path("recommendations");
        if (!items.isArray() || items.size() != RECOMMENDATION_COUNT) {
            throw new AiClientException("AI 커리큘럼 추천은 정확히 5개여야 합니다.");
        }

        Set<Long> templateIds = new LinkedHashSet<>();
        for (JsonNode item : items) {
            templateIds.add(item.path("trainingTemplateId").asLong());
        }
        if (templateIds.size() != RECOMMENDATION_COUNT || templateIds.contains(0L)) {
            throw new AiClientException("AI 커리큘럼 추천 템플릿 ID가 유효하지 않습니다.");
        }

        Map<Long, TrainingTemplateEntity> templates = new HashMap<>();
        templateRepository.findAllById(templateIds)
                .forEach(template -> templates.put(template.getId(), template));
        if (templates.size() != RECOMMENDATION_COUNT) {
            throw new AiClientException("AI가 존재하지 않는 훈련 템플릿을 추천했습니다.");
        }

        List<AiCurriculumRecommendationResponse.Recommendation> recommendations =
                new ArrayList<>();
        for (JsonNode item : items) {
            Long templateId = item.path("trainingTemplateId").asLong();
            TrainingTemplateEntity template = Objects.requireNonNull(templates.get(templateId));
            recommendations.add(new AiCurriculumRecommendationResponse.Recommendation(
                    item.path("sequenceNo").asInt(),
                    templateId,
                    template.getName(),
                    item.path("role").asText(),
                    item.path("recommendedDifficulty").asInt(),
                    item.path("score").asDouble(),
                    textList(item.path("targetFeatureCodes")),
                    textList(item.path("reasonCodes")),
                    item.path("rationale").asText()
            ));
        }

        return new AiCurriculumRecommendationResponse(
                response.path("recommendationProvider").asText(),
                response.path("dataSufficiency").asText(),
                response.path("currentStage").asInt(),
                response.path("maximumAllowedStage").asInt(),
                response.path("stageRationale").asText(),
                List.copyOf(recommendations),
                textList(response.path("warnings"))
        );
    }

    private List<String> textList(JsonNode values) {
        if (!values.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }
}
