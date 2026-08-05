package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import com.iread.backend.training.generation.TrainingType;
import com.iread.backend.training.input.TrainingInputPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.Set;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;

@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iread.training-template-seed.enabled", havingValue = "true")
public class TrainingTemplateDataInitializer implements ApplicationRunner {

    private static final String SEED_RESOURCE = "training-templates.json";
    private static final String PROMPT_VERSION = "TRAINING_PROMPT_V2";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        JsonNode root = readSeed();
        root.path("curriculumUnits").forEach(this::insertCurriculumUnitWhenMissing);
        root.path("templates").forEach(this::insertTemplateWhenMissing);
    }

    private JsonNode readSeed() {
        try (var input = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("훈련 템플릿 seed 파일을 읽지 못했습니다.", exception);
        }
    }

    private void insertCurriculumUnitWhenMissing(JsonNode seed) {
        long id = seed.path("id").asLong();
        if (exists("curriculum_units", id)) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO curriculum_units (id, unit_name, sequence_no) VALUES (?, ?, ?)",
                id,
                seed.path("name").asText(),
                seed.path("sequenceNo").asInt()
        );
    }

    private void insertTemplateWhenMissing(JsonNode seed) {
        long id = seed.path("id").asLong();
        JsonNode prompt = seed.path("prompt").deepCopy();
        TrainingType type = TrainingType.from(prompt.path("trainingType").asText());
        TrainingInputPolicy.parseAndValidate(type, prompt.path("requiredInputs"));
        ((tools.jackson.databind.node.ObjectNode) prompt).put("promptVersion", PROMPT_VERSION);
        String serializedPrompt = writePrompt(prompt);
        if (exists("training_templates", id)) {
            synchronizeSeedContract(id, type, prompt);
            synchronizeName(id, seed.path("name").asText());
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO training_templates (id, curriculum_unit_id, name, prompt, sequence_no)
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                seed.path("curriculumUnitId").asLong(),
                seed.path("name").asText(),
                serializedPrompt,
                seed.path("sequenceNo").asInt()
        );
    }

    /** 읽기 훈련 통합으로 개명한 템플릿. 그 외 기존 행의 이름은 seed가 덮어쓰지 않는다. */
    private static final Set<Long> CONSOLIDATION_RENAMED_TEMPLATE_IDS = Set.of(25L, 33L);

    private void synchronizeName(long id, String name) {
        if (!CONSOLIDATION_RENAMED_TEMPLATE_IDS.contains(id)) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE training_templates SET name = ? WHERE id = ? AND name <> ?",
                name,
                id,
                name
        );
    }

    private void synchronizeSeedContract(
            long id,
            TrainingType expectedType,
            JsonNode seedPrompt
    ) {
        String serialized = jdbcTemplate.queryForObject(
                "SELECT prompt FROM training_templates WHERE id = ?",
                String.class,
                id
        );
        JsonNode current = readPrompt(serialized);
        if (!(current instanceof ObjectNode currentPrompt)) {
            throw new IllegalStateException("기존 훈련 템플릿 prompt는 JSON 객체여야 합니다.");
        }
        TrainingType currentType = TrainingType.from(
                currentPrompt.path("trainingType").asText()
        );
        if (currentType != expectedType) {
            throw new IllegalStateException(
                    "기존 훈련 템플릿의 trainingType이 seed와 일치하지 않습니다: " + id
            );
        }
        for (String field : new String[]{
                "requiredInputs",
                "additionalPrompt",
                "outputTemplate",
                "supportedFeatureCategories",
                "supportedScopes"
        }) {
            currentPrompt.set(field, seedPrompt.path(field).deepCopy());
        }
        currentPrompt.put("promptVersion", PROMPT_VERSION);
        jdbcTemplate.update(
                "UPDATE training_templates SET prompt = ? WHERE id = ?",
                writePrompt(currentPrompt),
                id
        );
    }

    private boolean exists(String table, long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    private String writePrompt(JsonNode prompt) {
        try {
            return objectMapper.writeValueAsString(prompt);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 템플릿 prompt JSON 생성에 실패했습니다.", exception);
        }
    }

    private JsonNode readPrompt(String prompt) {
        try {
            return objectMapper.readTree(prompt);
        } catch (Exception exception) {
            throw new IllegalStateException("기존 훈련 템플릿 prompt JSON을 읽지 못했습니다.", exception);
        }
    }
}
