package com.iread.backend.training.config;

import com.iread.backend.readingfeature.config.ReadingFeatureDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "iread.reading-feature-seed.enabled=true",
        "iread.training-template-seed.enabled=true"
})
@Transactional
class TrainingSeedInitializerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReadingFeatureDataInitializer readingFeatureDataInitializer;

    @Autowired
    private TrainingTemplateDataInitializer trainingTemplateDataInitializer;

    @Test
    void initializesAllReadingFeaturesAndTrainingTypes() throws Exception {
        assertThat(count("reading_features")).isEqualTo(113);
        assertThat(count("curriculum_units")).isEqualTo(8);
        assertThat(count("training_templates")).isEqualTo(31);

        String prompt = jdbcTemplate.queryForObject(
                "SELECT prompt FROM training_templates WHERE id = 34",
                String.class
        );
        var json = objectMapper.readTree(prompt);
        assertThat(json.path("trainingType").asText()).isEqualTo("SHORT_STORY_READING");
        assertThat(json.path("requiredInputs")).containsExactly(
                objectMapper.getNodeFactory().textNode("VOICE"),
                objectMapper.getNodeFactory().textNode("GAZE")
        );
        assertThat(json.path("promptVersion").asText()).isEqualTo("TRAINING_PROMPT_V2");
        assertThat(json.path("additionalPrompt").asText()).isNotBlank();
        assertThat(json.path("outputTemplate").path("type").asText())
                .isEqualTo("SHORT_STORY_READING");
        assertThat(json.path("supportedFeatureCategories").isArray()).isTrue();
        assertThat(json.path("supportedScopes").isArray()).isTrue();
    }

    @Test
    void rerunDoesNotOverwriteExistingRows() {
        jdbcTemplate.update("UPDATE reading_features SET feature_name = '교사 관리 이름' WHERE id = 1");
        jdbcTemplate.update("UPDATE training_templates SET name = '교사 관리 템플릿' WHERE id = 1");
        jdbcTemplate.update("""
                UPDATE training_templates
                SET prompt = '{"trainingType":"VOWEL_TRACE","customSetting":"유지"}'
                WHERE id = 1
                """);

        readingFeatureDataInitializer.run(null);
        trainingTemplateDataInitializer.run(null);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT feature_name FROM reading_features WHERE id = 1",
                String.class
        )).isEqualTo("교사 관리 이름");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM training_templates WHERE id = 1",
                String.class
        )).isEqualTo("교사 관리 템플릿");
        String prompt = jdbcTemplate.queryForObject(
                "SELECT prompt FROM training_templates WHERE id = 1",
                String.class
        );
        assertThat(prompt).isNotNull();
        var promptJson = objectMapper.readTree(prompt);
        assertThat(promptJson.path("customSetting").asText()).isEqualTo("유지");
        assertThat(promptJson.path("requiredInputs")).containsExactly(
                objectMapper.getNodeFactory().textNode("VOICE"),
                objectMapper.getNodeFactory().textNode("GAZE")
        );
        assertThat(count("reading_features")).isEqualTo(113);
        assertThat(count("training_templates")).isEqualTo(31);
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
