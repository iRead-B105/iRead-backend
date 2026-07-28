package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iread.training-template-seed.enabled", havingValue = "true", matchIfMissing = true)
public class TrainingTemplateDataInitializer implements ApplicationRunner {

    private static final String SEED_RESOURCE = "training-templates.json";
    private static final String PROMPT_VERSION = "TRAINING_PROMPT_V1";

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
        if (exists("training_templates", id)) {
            return;
        }

        JsonNode prompt = seed.path("prompt").deepCopy();
        ((tools.jackson.databind.node.ObjectNode) prompt).put("promptVersion", PROMPT_VERSION);
        jdbcTemplate.update("""
                INSERT INTO training_templates (id, curriculum_unit_id, name, prompt, sequence_no)
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                seed.path("curriculumUnitId").asLong(),
                seed.path("name").asText(),
                writePrompt(prompt),
                seed.path("sequenceNo").asInt()
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
}
