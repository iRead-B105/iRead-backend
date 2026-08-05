package com.iread.backend.training.config;

import com.iread.backend.readingfeature.config.ReadingFeatureDataInitializer;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "iread.reading-feature-seed.enabled=true",
        "iread.training-template-seed.enabled=true",
        "iread.demo-voice-first-training.enabled=true"
})
@Transactional
class DemoVoiceFirstTrainingInitializerTest {

    private static final long CURRICULUM_ID = 180003L;
    private static final long STUDENT_ID = 2103L;
    private static final long TRAINING_ID = 181031L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReadingFeatureDataInitializer readingFeatureDataInitializer;

    @Autowired
    private TrainingTemplateDataInitializer trainingTemplateDataInitializer;

    @Autowired
    private DemoVoiceFirstTrainingInitializer initializer;

    @BeforeEach
    void seed() {
        readingFeatureDataInitializer.run(new DefaultApplicationArguments());
        trainingTemplateDataInitializer.run(new DefaultApplicationArguments());
        jdbcTemplate.update("""
                INSERT INTO teachers (id, email, password, name, created_at)
                VALUES (9001, 'voice-demo@iread.local', 'x', '데모', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO students (id, teacher_id, name, birthday, gender, school, created_at)
                VALUES (?, 9001, '박서아', '2019-02-18', 'girl', '데모초등학교', CURRENT_TIMESTAMP)
                """, STUDENT_ID);
        jdbcTemplate.update("""
                INSERT INTO daily_curriculums (id, student_id, status, created_at)
                VALUES (?, ?, 'NOT_STARTED', CURRENT_TIMESTAMP)
                """, CURRICULUM_ID, STUDENT_ID);
        // V2 시드와 같이 마이크가 필요 없는 선택형 템플릿으로 시작한다.
        jdbcTemplate.update("""
                INSERT INTO trainings
                    (id, training_template_id, daily_curriculum_id, sequence_no,
                     created_at, status)
                VALUES (?, 4, ?, 1, CURRENT_TIMESTAMP, 'NOT_STARTED')
                """, TRAINING_ID, CURRICULUM_ID);
        jdbcTemplate.update("""
                INSERT INTO training_datas (train_id, generated_data, created_at)
                VALUES (?, '{"schemaVersion":2,"questions":[]}', CURRENT_TIMESTAMP)
                """, TRAINING_ID);
    }

    @Test
    void 첫_훈련을_음성_문항으로_바꾸고_문항을_다시_만든다() {
        initializer.run(new DefaultApplicationArguments());

        Long templateId = jdbcTemplate.queryForObject(
                "SELECT training_template_id FROM trainings WHERE id = ?",
                Long.class,
                TRAINING_ID
        );
        assertThat(templateId).isEqualTo(DemoVoiceFirstTrainingInitializer.VOICE_TEMPLATE_ID);

        JsonNode question = firstQuestion();
        assertThat(question.path("type").asText()).isEqualTo("WORD_READING");
        assertThat(TrainingInputPolicy.forQuestion(question))
                .contains(TrainingInputType.VOICE);
        // 녹음 제출이 analysisTargets로 대상 낱말을 찾으므로 분석 결과가 있어야 한다.
        assertThat(question.path("analysisTargets")).isNotEmpty();
        assertThat(question.path("answer").path("expectedText").asText()).isNotBlank();
    }

    @Test
    void 이미_음성_문항이면_다시_만들지_않는다() {
        initializer.run(new DefaultApplicationArguments());
        String first = generatedData();

        initializer.run(new DefaultApplicationArguments());

        assertThat(generatedData()).isEqualTo(first);
    }

    @Test
    void 기존_낱말_읽기_훈련이_있으면_데이터_연결을_유지하고_첫_순서로_옮긴다() {
        jdbcTemplate.update("""
                INSERT INTO trainings
                    (id, training_template_id, daily_curriculum_id, sequence_no,
                     created_at, status)
                VALUES (181033, 22, ?, 3, CURRENT_TIMESTAMP, 'NOT_READY')
                """, CURRICULUM_ID);

        initializer.run(new DefaultApplicationArguments());

        Integer voiceSequence = jdbcTemplate.queryForObject(
                "SELECT sequence_no FROM trainings WHERE id = 181033",
                Integer.class
        );
        String voiceStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM trainings WHERE id = 181033",
                String.class
        );
        Integer previousFirstSequence = jdbcTemplate.queryForObject(
                "SELECT sequence_no FROM trainings WHERE id = ?",
                Integer.class,
                TRAINING_ID
        );
        assertThat(voiceSequence).isEqualTo(1);
        assertThat(voiceStatus).isEqualTo("NOT_STARTED");
        assertThat(previousFirstSequence).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_datas WHERE train_id = 181033",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void 다른_데모_학습자의_교육과정은_건드리지_않는다() {
        jdbcTemplate.update("""
                INSERT INTO students (id, teacher_id, name, birthday, gender, school, created_at)
                VALUES (2002, 9001, '한결', '2018-09-21', 'boy', '데모초등학교', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO daily_curriculums (id, student_id, status, created_at)
                VALUES (180002, 2002, 'NOT_STARTED', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO trainings
                    (id, training_template_id, daily_curriculum_id, sequence_no,
                     created_at, status)
                VALUES (181021, 4, 180002, 1, CURRENT_TIMESTAMP, 'NOT_STARTED')
                """);

        initializer.run(new DefaultApplicationArguments());

        Long untouched = jdbcTemplate.queryForObject(
                "SELECT training_template_id FROM trainings WHERE id = 181021",
                Long.class
        );
        assertThat(untouched).isEqualTo(4L);
    }

    private String generatedData() {
        return jdbcTemplate.queryForObject(
                "SELECT generated_data FROM training_datas WHERE train_id = ?",
                String.class,
                TRAINING_ID
        );
    }

    private JsonNode firstQuestion() {
        // 테스트 DB는 json 컬럼에 넣은 문자열을 JSON 문자열로 한 번 더 감싼다.
        // MySQL은 객체로 저장하므로 두 형태를 모두 받아 준다.
        JsonNode root = objectMapper.readTree(generatedData());
        if (root.isTextual()) {
            root = objectMapper.readTree(root.asString());
        }
        return root.path("questions").path(0);
    }
}
