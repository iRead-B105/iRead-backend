package com.iread.backend.training.config;

import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 데모 학습자 한 명의 첫 훈련을 음성 문항으로 바꾼다.
 *
 * <p>V2 시드의 데모 교육과정은 마이크 없이 끝낼 수 있도록 requiredInputs가 빈 템플릿만 쓴다.
 * 그래서 발음 평가를 화면에서 확인하려면 매번 DB를 손봐야 했고 재시작하면 되돌아갔다.
 * 한결(2002)의 첫 훈련만 음성 문항으로 고정하고 나머지 데모 학습자는 그대로 둔다.
 */
@Component
@Order(45)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-voice-first-training.enabled",
        havingValue = "true"
)
public class DemoVoiceFirstTrainingInitializer implements ApplicationRunner {

    static final long TARGET_CURRICULUM_ID = 180002L;
    static final int TARGET_SEQUENCE_NO = 1;
    /** 문장 따라 읽기. 마이크 버튼 하나로 녹음을 끝낼 수 있어 발음 평가 확인에 가장 단순하다. */
    static final long VOICE_TEMPLATE_ID = 30L;

    private final JdbcTemplate jdbcTemplate;
    private final TrainingRepository trainingRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final PersonalizedTrainingGenerationService generationService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long trainingId = findTargetTrainingId();
        if (trainingId == null || alreadyVoiceTraining(trainingId)) {
            return;
        }

        jdbcTemplate.update(
                "UPDATE trainings SET training_template_id = ? WHERE id = ?",
                VOICE_TEMPLATE_ID,
                trainingId
        );
        // JDBC로 바꾼 템플릿을 영속성 컨텍스트가 모르므로 비우고 다시 읽는다.
        entityManager.flush();
        entityManager.clear();

        TrainingEntity training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new IllegalStateException(
                        "데모 음성 훈련을 찾을 수 없습니다: " + trainingId
                ));
        ObjectNode generated = generationService.generate(training);
        keepFirstQuestion(generated);
        String serialized = writeJson(generated);

        trainingDataRepository.findByTrainingId(trainingId)
                .ifPresentOrElse(
                        data -> data.updateGeneratedData(serialized),
                        () -> trainingDataRepository.save(
                                new TrainingDataEntity(training, serialized)
                        )
                );
        trainingDataRepository.flush();
    }

    private Long findTargetTrainingId() {
        return jdbcTemplate.query(
                """
                SELECT id
                  FROM trainings
                 WHERE daily_curriculum_id = ?
                   AND sequence_no = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                TARGET_CURRICULUM_ID,
                TARGET_SEQUENCE_NO
        );
    }

    private boolean alreadyVoiceTraining(long trainingId) {
        Long templateId = jdbcTemplate.queryForObject(
                "SELECT training_template_id FROM trainings WHERE id = ?",
                Long.class,
                trainingId
        );
        return templateId != null
                && templateId == VOICE_TEMPLATE_ID
                && trainingDataRepository.findByTrainingId(trainingId).isPresent();
    }

    private void keepFirstQuestion(ObjectNode generated) {
        ArrayNode questions = generated.withArray("questions");
        if (questions.isEmpty()) {
            throw new IllegalStateException("생성된 데모 음성 훈련 문항이 없습니다.");
        }
        while (questions.size() > 1) {
            questions.remove(questions.size() - 1);
        }
    }

    private String writeJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JacksonException exception) {
            throw new IllegalStateException("데모 음성 훈련 문항을 직렬화하지 못했습니다.", exception);
        }
    }
}
