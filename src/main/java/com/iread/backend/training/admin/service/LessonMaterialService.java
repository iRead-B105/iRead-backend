package com.iread.backend.training.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.dto.req.UpdateLessonMaterialRequest;
import com.iread.backend.training.admin.dto.res.LessonMaterialResponse;
import com.iread.backend.training.admin.dto.res.SaveLessonMaterialResponse;
import com.iread.backend.training.admin.exception.LessonMaterialException;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.generation.CandidateValidationIssue;
import com.iread.backend.training.generation.CandidateValidationResult;
import com.iread.backend.training.generation.TrainingCandidateRequest;
import com.iread.backend.training.generation.TrainingCandidateResponse;
import com.iread.backend.training.generation.TrainingCandidateValidator;
import com.iread.backend.training.generation.TrainingQuestionAssembler;
import com.iread.backend.training.generation.TrainingType;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonMaterialService {

    private static final int SCHEMA_VERSION = 2;
    private static final int MAX_JSON_DEPTH = 8;
    private static final int MAX_JSON_ITEMS = 50;
    private static final int MAX_JSON_TEXT_LENGTH = 2_000;
    private static final int MAX_JSON_SERIALIZED_LENGTH = 20_000;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final TrainingCandidateValidator candidateValidator;
    private final TrainingQuestionAssembler questionAssembler;
    private final ObjectMapper objectMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public LessonMaterialResponse getLessonMaterial(
            Long teacherId,
            Long studentId,
            Long trainingId
    ) {
        validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository
                .findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRAINING_NOT_FOUND",
                        "Training was not found."
                ));
        ObjectNode root = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::parseObject)
                .orElseGet(objectMapper::createObjectNode);
        return toResponse(training, root);
    }

    @Transactional
    public SaveLessonMaterialResponse updateLessonMaterial(
            Long teacherId,
            Long studentId,
            Long trainingId,
            UpdateLessonMaterialRequest request
    ) {
        validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRAINING_NOT_FOUND",
                        "Training was not found."
                ));
        if (!training.isEditable()) {
            throw new LessonMaterialException(
                    HttpStatus.CONFLICT,
                    "TRAINING_NOT_EDITABLE",
                    "Lesson materials cannot be changed after training starts."
            );
        }

        TrainingDataEntity data = trainingDataRepository.findByTrainingId(trainingId)
                .orElseGet(() -> trainingDataRepository.save(
                        new TrainingDataEntity(training, "{}")
                ));
        ObjectNode current = parseObject(data.getGeneratedData());
        int currentRevision = revision(current);
        if (request.revision() != currentRevision) {
            throw new LessonMaterialException(
                    HttpStatus.CONFLICT,
                    "LESSON_MATERIAL_REVISION_CONFLICT",
                    "The lesson material was changed. Reload the latest revision.",
                    Map.of(
                            "requestedRevision", request.revision(),
                            "currentRevision", currentRevision
                    )
            );
        }

        ObjectNode prompt = parsePrompt(training);
        TrainingType trainingType = TrainingType.from(prompt.path("trainingType").asText());
        Set<TrainingInputType> requiredInputs = TrainingInputPolicy.resolve(
                trainingType,
                prompt.path("requiredInputs")
        );
        List<ValidationError> errors = new ArrayList<>();
        List<JsonNode> candidates = buildCandidates(
                request.materials(),
                trainingType,
                prompt.path("outputTemplate").path("data").path(0),
                errors
        );
        validateCandidates(trainingType, prompt, candidates, request.materials().size(), errors);

        Map<Integer, List<String>> targetCodesByQuestionNo = targetCodes(current);
        List<ObjectNode> normalized = new ArrayList<>();
        if (errors.isEmpty()) {
            for (int index = 0; index < request.materials().size(); index++) {
                UpdateLessonMaterialRequest.Material material = request.materials().get(index);
                List<String> targetCodes = targetCodesByQuestionNo
                        .getOrDefault(material.questionNo(), List.of());
                TrainingQuestionAssembler.AssembledQuestion assembled = questionAssembler.assemble(
                        index + 1,
                        trainingType,
                        candidates.get(index),
                        targetCodes,
                        requiredInputs
                );
                if (!assembled.question().path("answer").equals(material.answer())) {
                    errors.add(new ValidationError(
                            material.questionNo(),
                            "materials[" + index + "].answer",
                            "ANSWER_MISMATCH",
                            "The answer must match the question content."
                    ));
                }
                validateFeaturePolicy(
                        material.questionNo(),
                        index,
                        assembled.featureCodes(),
                        targetCodes,
                        stringValues(prompt.path("excludedFeatures")),
                        errors
                );
                ObjectNode question = assembled.question();
                if (material.presentation() != null) {
                    question.set("presentation", material.presentation().deepCopy());
                }
                normalized.add(question);
            }
        }
        if (!errors.isEmpty()) {
            throw validationFailed(errors);
        }

        int nextRevision = currentRevision + 1;
        ObjectNode updated = (ObjectNode) current.deepCopy();
        updated.put("schemaVersion", SCHEMA_VERSION);
        updated.put("revision", nextRevision);
        ArrayNode questions = updated.putArray("questions");
        normalized.forEach(questions::add);

        ObjectNode metadata = updated.withObject("generationMetadata");
        metadata.put("source", "MANUAL");
        metadata.put("editedAt", OffsetDateTime.now(SERVICE_ZONE).toString());
        metadata.put("trainingTemplateId", training.getTrainingTemplate().getId());
        ObjectNode validation = updated.withObject("validationResult");
        validation.put("passed", true);
        validation.putArray("issues");

        data.updateGeneratedData(writeJson(updated));
        training.markReady();
        training.getDailyCurriculum().markContentChanged();
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "CONTENT_UPDATED"
        );
        List<LessonMaterialResponse.Material> responseMaterials = toMaterials(updated);
        return new SaveLessonMaterialResponse(
                training.getId(),
                nextRevision,
                OffsetDateTime.now(SERVICE_ZONE),
                "MANUAL",
                responseMaterials
        );
    }

    private List<JsonNode> buildCandidates(
            List<UpdateLessonMaterialRequest.Material> materials,
            TrainingType trainingType,
            JsonNode candidateTemplate,
            List<ValidationError> errors
    ) {
        List<JsonNode> candidates = new ArrayList<>();
        for (int index = 0; index < materials.size(); index++) {
            UpdateLessonMaterialRequest.Material material = materials.get(index);
            int itemIndex = index;
            validateJsonPayload(material.questionNo(), "materials[" + index + "].presentation", material.presentation(), true, errors);
            validateJsonPayload(material.questionNo(), "materials[" + index + "].content", material.content(), false, errors);
            validateJsonPayload(material.questionNo(), "materials[" + index + "].answer", material.answer(), false, errors);
            if (material.questionNo() != index + 1) {
                errors.add(new ValidationError(
                        material.questionNo(),
                        "materials[" + index + "].questionNo",
                        "QUESTION_ORDER_MISMATCH",
                        "questionNo must match the one-based material order."
                ));
            }

            if (!trainingType.name().equals(material.questionType())) {
                errors.add(new ValidationError(
                        material.questionNo(),
                        "materials[" + index + "].questionType",
                        "TYPE_MISMATCH",
                        "Question type must match the training template."
                ));
            }
            if (!material.content().isObject()) {
                errors.add(new ValidationError(
                        material.questionNo(),
                        "materials[" + index + "].content",
                        "TYPE_MISMATCH",
                        "content must be a JSON object."
                ));
            }
            if (!material.answer().isObject()) {
                errors.add(new ValidationError(
                        material.questionNo(),
                        "materials[" + index + "].answer",
                        "TYPE_MISMATCH",
                        "answer must be a JSON object."
                ));
            }
            if (material.presentation() != null
                    && !material.presentation().isNull()
                    && !material.presentation().isObject()) {
                errors.add(new ValidationError(
                        material.questionNo(),
                        "materials[" + index + "].presentation",
                        "TYPE_MISMATCH",
                        "presentation must be a JSON object."
                ));
            }
            if (material.presentation() != null && material.presentation().isObject()) {
                validatePresentation(material.questionNo(), index, material.presentation(), errors);
            }

            ObjectNode candidate = objectMapper.createObjectNode();
            if (material.content().isObject()) {
                material.content().properties().forEach(field ->
                        candidate.set(field.getKey(), field.getValue().deepCopy()));
            }
            if (material.answer().isObject()) {
                material.answer().properties().forEach(field -> {
                    if (candidate.has(field.getKey())
                            && !candidate.get(field.getKey()).equals(field.getValue())) {
                        errors.add(new ValidationError(
                                material.questionNo(),
                                "materials[" + itemIndex + "].answer." + field.getKey(),
                                "CONFLICTING_FIELD",
                                "content and answer cannot contain conflicting values."
                        ));
                    }
                    if (candidateTemplate.has(field.getKey())) {
                        candidate.set(field.getKey(), field.getValue().deepCopy());
                    }
                });
            }
            candidates.add(candidate);
        }
        return candidates;
    }

    private void validateCandidates(
            TrainingType trainingType,
            ObjectNode prompt,
            List<JsonNode> candidates,
            int materialCount,
            List<ValidationError> errors
    ) {
        ArrayNode padded = objectMapper.createArrayNode();
        candidates.forEach(value -> padded.add(value.deepCopy()));
        while (padded.size() < 5) {
            padded.add(candidates.getFirst().deepCopy());
        }
        TrainingCandidateRequest validationRequest = new TrainingCandidateRequest(
                "lesson-material-validation",
                SCHEMA_VERSION,
                trainingType,
                5,
                2,
                List.of(),
                List.of(),
                "",
                prompt.path("outputTemplate")
        );
        CandidateValidationResult result = candidateValidator.validate(
                validationRequest,
                new TrainingCandidateResponse(trainingType.name(), padded)
        );
        for (CandidateValidationIssue issue : result.issues()) {
            if (issue.dataIndex() < 0 || issue.dataIndex() >= materialCount) {
                continue;
            }
            int index = issue.dataIndex();
            errors.add(new ValidationError(
                    index + 1,
                    issue.path().replace("$.data[" + index + "]", "materials[" + index + "]"),
                    issue.type(),
                    issue.message()
            ));
        }
    }

    private void validatePresentation(
            int questionNo,
            int index,
            JsonNode presentation,
            List<ValidationError> errors
    ) {
        if (presentation.has("activityName")) {
            validateRequiredText(questionNo, "materials[" + index + "].presentation.activityName",
                    presentation.get("activityName"), 100, errors);
        }
        if (presentation.has("instruction")) {
            validateRequiredText(questionNo, "materials[" + index + "].presentation.instruction",
                    presentation.get("instruction"), 500, errors);
        }
    }

    private void validateRequiredText(
            int questionNo,
            String path,
            JsonNode value,
            int maxLength,
            List<ValidationError> errors
    ) {
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            errors.add(new ValidationError(questionNo, path, "REQUIRED", "A non-blank text value is required."));
        } else if (value.asText().length() > maxLength || !isSafeText(value.asText())) {
            errors.add(new ValidationError(questionNo, path, "INVALID_TEXT", "The text value is too long or contains invalid characters."));
        }
    }

    private void validateJsonPayload(
            int questionNo,
            String path,
            JsonNode value,
            boolean nullable,
            List<ValidationError> errors
    ) {
        if (value == null || value.isNull()) {
            if (!nullable) errors.add(new ValidationError(questionNo, path, "REQUIRED", "A JSON object is required."));
            return;
        }
        if (value.toString().length() > MAX_JSON_SERIALIZED_LENGTH) {
            errors.add(new ValidationError(questionNo, path, "PAYLOAD_TOO_LARGE", "The JSON value is too large."));
            return;
        }
        validateJsonNode(questionNo, path, value, 0, errors);
    }

    private void validateJsonNode(
            int questionNo,
            String path,
            JsonNode value,
            int depth,
            List<ValidationError> errors
    ) {
        if (depth > MAX_JSON_DEPTH) {
            errors.add(new ValidationError(questionNo, path, "JSON_TOO_DEEP", "The JSON value is nested too deeply."));
            return;
        }
        if (value.isTextual()) {
            if (value.asText().length() > MAX_JSON_TEXT_LENGTH || !isSafeText(value.asText())) {
                errors.add(new ValidationError(questionNo, path, "INVALID_TEXT", "The text value is too long or contains invalid characters."));
            }
            return;
        }
        if (value.isArray()) {
            if (value.size() > MAX_JSON_ITEMS) {
                errors.add(new ValidationError(questionNo, path, "TOO_MANY_ITEMS", "The JSON array has too many items."));
                return;
            }
            for (int index = 0; index < value.size(); index++) {
                validateJsonNode(questionNo, path + "[" + index + "]", value.get(index), depth + 1, errors);
            }
            return;
        }
        if (value.isObject()) {
            if (value.size() > MAX_JSON_ITEMS) {
                errors.add(new ValidationError(questionNo, path, "TOO_MANY_FIELDS", "The JSON object has too many fields."));
                return;
            }
            for (Map.Entry<String, JsonNode> field : value.properties()) {
                if (field.getKey().length() > 100 || !isSafeText(field.getKey())) {
                    errors.add(new ValidationError(
                            questionNo,
                            path,
                            "INVALID_FIELD_NAME",
                            "The JSON object contains an invalid field name."
                    ));
                    continue;
                }
                validateJsonNode(
                        questionNo,
                        path + "." + field.getKey(),
                        field.getValue(),
                        depth + 1,
                        errors
                );
            }
        }
    }

    private boolean isSafeText(String value) {
        return value.codePoints().noneMatch(codePoint ->
                Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\r' && codePoint != '\t');
    }

    private void validateFeaturePolicy(
            int questionNo,
            int index,
            Set<String> actual,
            List<String> targets,
            List<String> excluded,
            List<ValidationError> errors
    ) {
        for (String target : targets) {
            if (actual.stream().noneMatch(code -> featureMatches(code, target))) {
                errors.add(new ValidationError(
                        questionNo,
                        "materials[" + index + "].content",
                        "TARGET_FEATURE_MISSING",
                        "Question must contain target feature: " + target
                ));
            }
        }
        for (String forbidden : excluded) {
            if (actual.stream().anyMatch(code -> featureMatches(code, forbidden))) {
                errors.add(new ValidationError(
                        questionNo,
                        "materials[" + index + "].content",
                        "EXCLUDED_FEATURE_FOUND",
                        "Question contains excluded feature: " + forbidden
                ));
            }
        }
    }

    private boolean featureMatches(String actual, String requested) {
        return actual.equals(requested)
                || actual.startsWith(requested + ".")
                || requested.startsWith(actual + ".");
    }

    private LessonMaterialResponse toResponse(TrainingEntity training, ObjectNode root) {
        return new LessonMaterialResponse(
                training.getId(),
                training.getTrainingTemplate().getId(),
                training.getTrainingTemplate().getName(),
                training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                training.getStatus().name(),
                root.path("schemaVersion").asInt(SCHEMA_VERSION),
                revision(root),
                training.isEditable(),
                toMaterials(root)
        );
    }

    private List<LessonMaterialResponse.Material> toMaterials(ObjectNode root) {
        List<LessonMaterialResponse.Material> result = new ArrayList<>();
        JsonNode questions = root.path("questions");
        if (!questions.isArray()) {
            return result;
        }
        for (int index = 0; index < questions.size(); index++) {
            JsonNode question = questions.get(index);
            TrainingType type = TrainingType.from(question.path("type").asText());
            List<String> requiredInputs = TrainingInputPolicy.expectedFor(type).stream()
                    .sorted()
                    .map(Enum::name)
                    .toList();
            result.add(new LessonMaterialResponse.Material(
                    index + 1,
                    type.name(),
                    responseType(type),
                    requiredInputs,
                    question.has("presentation") ? question.get("presentation").deepCopy() : null,
                    question.path("content").deepCopy(),
                    question.path("answer").deepCopy()
            ));
        }
        return List.copyOf(result);
    }

    private String responseType(TrainingType type) {
        return switch (type) {
            case VOWEL_TRACE, CONSONANT_TRACE, SYLLABLE_TRACE -> "TRACE";
            case PHONEME_BLEND, SYLLABLE_BLEND, SENTENCE_ASSEMBLY -> "ORDERING";
            case BASIC_SYLLABLE_BUILD, FINAL_SYLLABLE_BUILD, DOUBLE_FINAL_BUILD -> "COMPONENT_BUILD";
            case WORD_READING, NONWORD_READING, DIFFICULT_WORD_PREVIEW, SENTENCE_READING,
                    SHORT_PASSAGE_READING, SENTENCE_REPEAT, WORD_CHAIN_READING, PHRASE_READING,
                    REPEATED_SENTENCE_READING, SHORT_STORY_READING -> "AUDIO";
            case FILL_IN_THE_BLANK -> "FILL_IN_THE_BLANK";
            default -> "SINGLE_CHOICE";
        };
    }

    private Map<Integer, List<String>> targetCodes(ObjectNode root) {
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        root.path("questions").forEach(question -> result.put(
                question.path("questionNo").asInt(),
                stringValues(question.path("targetFeatureCodes"))
        ));
        return result;
    }

    private List<String> stringValues(JsonNode values) {
        if (!values.isArray()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        values.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) {
                result.add(value.asText());
            }
        });
        return List.copyOf(result);
    }

    private int revision(ObjectNode root) {
        return Math.max(0, root.path("revision").asInt(0));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        if (studentRepository.findByIdAndTeacherId(studentId, teacherId).isEmpty()) {
            throw new ResourceNotFoundException("Student was not found.");
        }
    }

    private ObjectNode parsePrompt(TrainingEntity training) {
        return parseObject(training.getTrainingTemplate().getPrompt());
    }

    private ObjectNode parseObject(String json) {
        try {
            JsonNode value = objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
            if (value instanceof ObjectNode object) {
                return object;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Stored training data is not a JSON object.");
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Lesson material could not be serialized.");
        }
    }

    private LessonMaterialException validationFailed(List<ValidationError> errors) {
        return new LessonMaterialException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "LESSON_MATERIAL_VALIDATION_FAILED",
                "Some lesson materials are invalid.",
                Map.of("errors", errors.stream().map(ValidationError::asMap).toList())
        );
    }

    private record ValidationError(int questionNo, String path, String reason, String message) {
        private Map<String, Object> asMap() {
            return Map.of(
                    "questionNo", questionNo,
                    "path", path,
                    "reason", reason,
                    "message", message
            );
        }
    }
}
