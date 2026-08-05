package com.iread.backend.training.generation;

import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonalizedTrainingGenerationService {

    public static final int GENERATED_DATA_SCHEMA_VERSION = 2;
    public static final String WEAKNESS_VERSION = "WEAKNESS_V1";
    private static final int QUESTION_COUNT = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final GenerationSource AI_SOURCE =
            new GenerationSource("AI", "AI_TRAINING_CANDIDATES_V1");
    private static final GenerationSource SEED_SOURCE =
            new GenerationSource("SEED", "DETERMINISTIC_TRAINING_SEED_V1");

    private final ObjectMapper objectMapper;
    private final TrainingCandidateProvider candidateProvider;
    private final TrainingCandidateValidator candidateValidator;
    private final TrainingQuestionAssembler questionAssembler;
    private final StudentFeatureProfileRepository profileRepository;

    private record GenerationSource(String provider, String model) {
    }

    /**
     * 초기 검사·첫날 커리큘럼처럼 AI 호출 없이 미리 정의된 데이터로 채워야 하는
     * 흐름에서 쓰는 시드 후보 공급자. ai.mock-generate 값과 무관하게 항상 쓸 수 있다.
     */
    private DeterministicTrainingCandidateProvider seedCandidateProvider;

    private TrainingCandidateProvider seedCandidateProvider() {
        if (seedCandidateProvider == null) {
            seedCandidateProvider = new DeterministicTrainingCandidateProvider(objectMapper);
        }
        return seedCandidateProvider;
    }

    public ObjectNode generate(TrainingEntity training) {
        return generate(training, candidateProvider, AI_SOURCE);
    }

    /** AI 호출 없이 시드 데이터로 훈련 문항을 생성한다. */
    public ObjectNode generateSeed(TrainingEntity training) {
        return generate(training, seedCandidateProvider(), SEED_SOURCE);
    }

    private ObjectNode generate(
            TrainingEntity training,
            TrainingCandidateProvider provider,
            GenerationSource source
    ) {
        ObjectNode prompt = parsePrompt(training.getTrainingTemplate().getPrompt());
        TrainingType type = TrainingType.from(prompt.path("trainingType").asText());
        Set<TrainingInputType> requiredInputs = TrainingInputPolicy.resolve(
                type,
                prompt.path("requiredInputs")
        );
        List<StudentFeatureProfileEntity> profiles = compatibleWeakProfiles(
                training.getDailyCurriculum().getStudent().getId(),
                prompt
        );
        List<TrainingTargetFeature> targets = profiles.stream()
                .limit(2)
                .map(this::toTarget)
                .toList();
        List<String> targetCodes = targets.stream().map(TrainingTargetFeature::featureCode).toList();
        List<String> excluded = stringValues(prompt.path("excludedFeatures"));
        int difficulty = difficulty(profiles);

        List<ObjectNode> accepted = new ArrayList<>();
        Set<String> acceptedCanonical = new HashSet<>();
        List<CandidateValidationIssue> allIssues = new ArrayList<>();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS && accepted.size() < QUESTION_COUNT; attempt++) {
            TrainingCandidateRequest request = new TrainingCandidateRequest(
                    "training-" + training.getId() + "-attempt-" + attempt,
                    GENERATED_DATA_SCHEMA_VERSION,
                    type,
                    QUESTION_COUNT,
                    difficulty,
                    targets,
                    excluded,
                    prompt.path("additionalPrompt").asText(),
                    prompt.path("outputTemplate")
            );
            TrainingCandidateResponse response = provider.generate(request);
            CandidateValidationResult structure = candidateValidator.validate(request, response);
            allIssues.addAll(structure.issues());

            Set<Integer> invalidIndices = new HashSet<>();
            boolean globalFailure = false;
            for (CandidateValidationIssue issue : structure.issues()) {
                if (issue.dataIndex() < 0) {
                    globalFailure = true;
                } else {
                    invalidIndices.add(issue.dataIndex());
                }
            }
            if (globalFailure) {
                continue;
            }
            for (int index = 0; index < response.data().size() && accepted.size() < QUESTION_COUNT; index++) {
                if (invalidIndices.contains(index)) {
                    continue;
                }
                JsonNode candidate = response.data().get(index);
                String canonical = candidate.toString();
                if (!acceptedCanonical.add(canonical)) {
                    continue;
                }
                TrainingQuestionAssembler.AssembledQuestion assembled = questionAssembler.assemble(
                        accepted.size() + 1,
                        type,
                        candidate,
                        targetCodes,
                        requiredInputs
                );
                List<CandidateValidationIssue> featureIssues = featureIssues(
                        index,
                        assembled.featureCodes(),
                        targetCodes,
                        excluded
                );
                if (!featureIssues.isEmpty()) {
                    allIssues.addAll(featureIssues);
                    acceptedCanonical.remove(canonical);
                    continue;
                }
                accepted.add(assembled.question());
            }
        }

        if (accepted.size() != QUESTION_COUNT) {
            throw new TrainingGenerationException(
                    "검증을 통과한 훈련 문항 5개를 생성하지 못했습니다.",
                    allIssues
            );
        }
        return envelope(training, prompt, profiles, accepted, source);
    }

    public ObjectNode generateTestQuestion(
            Long studentId,
            TrainingTemplateEntity template,
            String requestId
    ) {
        return generateTestQuestion(studentId, template, requestId, candidateProvider, AI_SOURCE);
    }

    /** AI 호출 없이 시드 데이터로 실력도전 문항을 생성한다. */
    public ObjectNode generateSeedTestQuestion(
            Long studentId,
            TrainingTemplateEntity template,
            String requestId
    ) {
        return generateTestQuestion(
                studentId, template, requestId, seedCandidateProvider(), SEED_SOURCE
        );
    }

    private ObjectNode generateTestQuestion(
            Long studentId,
            TrainingTemplateEntity template,
            String requestId,
            TrainingCandidateProvider provider,
            GenerationSource source
    ) {
        ObjectNode prompt = parsePrompt(template.getPrompt());
        TrainingType type = TrainingType.from(prompt.path("trainingType").asText());
        Set<TrainingInputType> requiredInputs = TrainingInputPolicy.resolve(
                type,
                prompt.path("requiredInputs")
        );
        List<StudentFeatureProfileEntity> profiles = compatibleWeakProfiles(
                studentId,
                prompt
        );
        List<TrainingTargetFeature> targets = profiles.stream()
                .limit(2)
                .map(this::toTarget)
                .toList();
        List<String> excluded = stringValues(prompt.path("excludedFeatures"));
        List<CandidateValidationIssue> allIssues = new ArrayList<>();

        ObjectNode question = tryGenerateTestQuestion(
                type, requiredInputs, profiles, targets, excluded, prompt, requestId,
                allIssues, provider
        );
        if (question == null && !targets.isEmpty()) {
            // 취약 특성을 반영한 문항 생성이 불가능한 조합(예: 겹받침 특성 ×
            // 해당 특성을 표현할 수 없는 템플릿)이면 검사 자체가 막히지 않도록
            // 특성 지정 없는 표준 문항으로 폴백한다.
            question = tryGenerateTestQuestion(
                    type, requiredInputs, profiles, List.of(), excluded,
                    prompt, requestId + "-fallback", allIssues, provider
            );
        }
        if (question == null) {
            throw new TrainingGenerationException(
                    "검증을 통과한 실력도전 문항을 생성하지 못했습니다.",
                    allIssues
            );
        }
        return testEnvelope(template, prompt, profiles, question, source);
    }

    private ObjectNode tryGenerateTestQuestion(
            TrainingType type,
            Set<TrainingInputType> requiredInputs,
            List<StudentFeatureProfileEntity> profiles,
            List<TrainingTargetFeature> targets,
            List<String> excluded,
            ObjectNode prompt,
            String requestId,
            List<CandidateValidationIssue> allIssues,
            TrainingCandidateProvider provider
    ) {
        List<String> targetCodes = targets.stream()
                .map(TrainingTargetFeature::featureCode)
                .toList();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            TrainingCandidateRequest request = new TrainingCandidateRequest(
                    requestId + "-attempt-" + attempt,
                    GENERATED_DATA_SCHEMA_VERSION,
                    type,
                    QUESTION_COUNT,
                    difficulty(profiles),
                    targets,
                    excluded,
                    prompt.path("additionalPrompt").asText(),
                    prompt.path("outputTemplate")
            );
            TrainingCandidateResponse response = provider.generate(request);
            CandidateValidationResult structure = candidateValidator.validate(request, response);
            allIssues.addAll(structure.issues());
            if (structure.issues().stream().anyMatch(issue -> issue.dataIndex() < 0)) {
                continue;
            }
            Set<Integer> invalidIndices = structure.issues().stream()
                    .filter(issue -> issue.dataIndex() >= 0)
                    .map(CandidateValidationIssue::dataIndex)
                    .collect(java.util.stream.Collectors.toSet());
            for (int index = 0; index < response.data().size(); index++) {
                if (invalidIndices.contains(index)) {
                    continue;
                }
                TrainingQuestionAssembler.AssembledQuestion assembled =
                        questionAssembler.assemble(
                                1,
                                type,
                                response.data().get(index),
                                targetCodes,
                                requiredInputs
                        );
                List<CandidateValidationIssue> featureIssues = featureIssues(
                        index,
                        assembled.featureCodes(),
                        targetCodes,
                        excluded
                );
                if (!featureIssues.isEmpty()) {
                    allIssues.addAll(featureIssues);
                    continue;
                }
                return assembled.question();
            }
        }
        return null;
    }

    private ObjectNode envelope(
            TrainingEntity training,
            ObjectNode prompt,
            List<StudentFeatureProfileEntity> profiles,
            List<ObjectNode> questions,
            GenerationSource source
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", GENERATED_DATA_SCHEMA_VERSION);

        ObjectNode metadata = root.putObject("generationMetadata");
        metadata.put("source", source.provider());
        metadata.put("provider", source.provider());
        metadata.put("model", source.model());
        metadata.put("promptVersion", prompt.path("promptVersion").asText("TRAINING_PROMPT_V2"));
        metadata.put("generatedAt", LocalDateTime.now().toString());
        metadata.put("trainingTemplateId", training.getTrainingTemplate().getId());

        ObjectNode snapshot = root.putObject("profileSnapshot");
        snapshot.put("analysisVersion", WEAKNESS_VERSION);
        ArrayNode features = snapshot.putArray("features");
        profiles.stream().limit(2).forEach(profile -> features.add(profileSnapshot(profile)));

        ArrayNode questionArray = root.putArray("questions");
        questions.forEach(questionArray::add);

        ObjectNode validation = root.putObject("validationResult");
        validation.put("passed", true);
        validation.put("analyzerVersion", KoreanTextAnalyzer.ANALYZER_VERSION);
        validation.put("g2pVersion", KoreanG2pEngine.G2P_VERSION);
        validation.put("ruleEngineVersion", KoreanG2pEngine.RULE_ENGINE_VERSION);
        validation.putArray("issues");
        return root;
    }

    private ObjectNode testEnvelope(
            TrainingTemplateEntity template,
            ObjectNode prompt,
            List<StudentFeatureProfileEntity> profiles,
            ObjectNode question,
            GenerationSource source
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", GENERATED_DATA_SCHEMA_VERSION);

        ObjectNode metadata = root.putObject("generationMetadata");
        metadata.put("source", source.provider());
        metadata.put("provider", source.provider());
        metadata.put("model", source.model());
        metadata.put("promptVersion", prompt.path("promptVersion").asText("TRAINING_PROMPT_V2"));
        metadata.put("generatedAt", LocalDateTime.now().toString());
        metadata.put("trainingTemplateId", template.getId());

        ObjectNode snapshot = root.putObject("profileSnapshot");
        snapshot.put("analysisVersion", WEAKNESS_VERSION);
        ArrayNode features = snapshot.putArray("features");
        profiles.stream().limit(2).forEach(profile -> features.add(profileSnapshot(profile)));

        root.putArray("questions").add(question);
        ObjectNode validation = root.putObject("validationResult");
        validation.put("passed", true);
        validation.put("analyzerVersion", KoreanTextAnalyzer.ANALYZER_VERSION);
        validation.put("g2pVersion", KoreanG2pEngine.G2P_VERSION);
        validation.put("ruleEngineVersion", KoreanG2pEngine.RULE_ENGINE_VERSION);
        validation.putArray("issues");
        return root;
    }

    private ObjectNode profileSnapshot(StudentFeatureProfileEntity profile) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("featureCode", profile.getReadingFeature().getFeatureCode());
        putUnit(value, "weaknessScore", profile.getWeaknessScore());
        putNullable(value, "accuracyRate", profile.getAccuracyRate());
        putScore(value, "avgPronunciationScore", profile.getAvgPronunciationScore());
        putNullable(value, "avgFixationDurationMs", profile.getAvgFixationDurationMs());
        putNullable(value, "avgRegressionCount", profile.getAvgRegressionCount());
        putNullable(value, "confidence", profile.getConfidence());
        putNullable(value, "evidenceCount", profile.getEvidenceCount());
        return value;
    }

    private List<StudentFeatureProfileEntity> compatibleWeakProfiles(Long studentId, ObjectNode prompt) {
        Set<String> categories = new HashSet<>(stringValues(prompt.path("supportedFeatureCategories")));
        Set<String> scopes = new HashSet<>(stringValues(prompt.path("supportedScopes")));
        return profileRepository.findAllByStudentIdOrderByWeaknessScoreDesc(studentId).stream()
                .filter(profile -> profile.getWeaknessScore() != null && profile.getWeaknessScore() >= 600)
                .filter(profile -> profile.getConfidence() != null
                        && profile.getConfidence().compareTo(BigDecimal.valueOf(0.6)) >= 0)
                .filter(profile -> profile.getEvidenceCount() != null && profile.getEvidenceCount() >= 5)
                .filter(profile -> categories.contains(profile.getReadingFeature().getCategory().name()))
                .filter(profile -> scopes.contains(profile.getReadingFeature().getScope().name()))
                .toList();
    }

    private TrainingTargetFeature toTarget(StudentFeatureProfileEntity profile) {
        return new TrainingTargetFeature(
                profile.getReadingFeature().getFeatureCode(),
                profile.getWeaknessScore() / 1000.0,
                profile.getConfidence().doubleValue(),
                profile.getEvidenceCount()
        );
    }

    private int difficulty(List<StudentFeatureProfileEntity> profiles) {
        if (profiles.isEmpty()) {
            return 2;
        }
        double accuracy = profiles.stream()
                .map(StudentFeatureProfileEntity::getAccuracyRate)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.5);
        int difficulty = accuracy >= 0.9 ? 5 : accuracy >= 0.75 ? 4 : accuracy >= 0.6 ? 3
                : accuracy >= 0.4 ? 2 : 1;
        boolean burden = profiles.stream().anyMatch(profile ->
                (profile.getAvgPronunciationScore() != null && profile.getAvgPronunciationScore() < 700)
                        || (profile.getAvgFixationDurationMs() != null
                        && profile.getAvgFixationDurationMs() > 1200)
                        || (profile.getAvgRegressionCount() != null
                        && profile.getAvgRegressionCount().compareTo(BigDecimal.valueOf(2)) > 0));
        return burden ? Math.max(1, difficulty - 1) : difficulty;
    }

    private List<CandidateValidationIssue> featureIssues(
            int dataIndex,
            Set<String> actual,
            List<String> targets,
            List<String> excluded
    ) {
        List<CandidateValidationIssue> issues = new ArrayList<>();
        for (String target : targets) {
            if (actual.stream().noneMatch(code -> featureMatches(code, target))) {
                issues.add(new CandidateValidationIssue(
                        dataIndex,
                        "$.data[" + dataIndex + "]",
                        "TARGET_FEATURE_MISSING",
                        "목표 특징이 없습니다: " + target
                ));
            }
        }
        for (String forbidden : excluded) {
            if (actual.stream().anyMatch(code -> featureMatches(code, forbidden))) {
                issues.add(new CandidateValidationIssue(
                        dataIndex,
                        "$.data[" + dataIndex + "]",
                        "EXCLUDED_FEATURE_FOUND",
                        "금지 특징이 포함됐습니다: " + forbidden
                ));
            }
        }
        return issues;
    }

    private boolean featureMatches(String actual, String requested) {
        return actual.equals(requested)
                || actual.startsWith(requested + ".")
                || requested.startsWith(actual + ".");
    }

    private ObjectNode parsePrompt(String json) {
        try {
            JsonNode value = objectMapper.readTree(json);
            if (value instanceof ObjectNode object) {
                return object;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("training_templates.prompt가 올바른 JSON 객체가 아닙니다.");
    }

    private List<String> stringValues(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        array.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) {
                result.add(value.asText());
            }
        });
        return List.copyOf(new LinkedHashSet<>(result));
    }

    private void putUnit(ObjectNode node, String field, Integer value) {
        if (value == null) node.putNull(field);
        else node.put(field, BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP));
    }

    private void putScore(ObjectNode node, String field, Integer value) {
        if (value == null) node.putNull(field);
        else node.put(field, BigDecimal.valueOf(value)
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP));
    }

    private void putNullable(ObjectNode node, String field, Object value) {
        if (value == null) {
            node.putNull(field);
        } else if (value instanceof Integer integer) {
            node.put(field, integer);
        } else if (value instanceof BigDecimal decimal) {
            node.put(field, decimal);
        } else {
            node.put(field, value.toString());
        }
    }
}
