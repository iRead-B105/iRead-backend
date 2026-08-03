package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데모 학습자 한 명의 첫 훈련을 음성 문항으로 바꾼다.
 *
 * <p>V2 시드의 데모 교육과정은 마이크 없이 끝낼 수 있도록 requiredInputs가 빈 템플릿만 쓴다.
 * 그래서 발음 평가를 화면에서 확인하려면 매번 DB를 손봐야 했고 재시작하면 되돌아갔다.
 * 박서아(2103)의 최신 시작 전 교육과정에서 낱말 읽기를 첫 훈련으로 고정한다.
 * 이미 낱말 읽기 훈련이 있으면 기존 데이터 연결을 유지한 채 순서만 바꾸고,
 * 없으면 첫 훈련의 템플릿을 낱말 읽기로 교체해 문항을 다시 만든다.
 */
@Component
@Order(45)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-voice-first-training.enabled",
        havingValue = "true"
)
public class DemoVoiceFirstTrainingInitializer implements ApplicationRunner {

    static final long TARGET_STUDENT_ID = 2103L;
    static final int TARGET_SEQUENCE_NO = 1;
    /** 낱말 읽기. 문장 읽기를 제외하면서 음성 평가와 시선 입력을 함께 확인할 수 있다. */
    static final long VOICE_TEMPLATE_ID = 22L;
    static final String VOICE_TRAINING_DATA = """
            {
              "schemaVersion": 2,
              "trainingType": "WORD_READING",
              "questions": [
                {
                  "questionId": "demo-seoa-word-reading-1",
                  "questionNo": 1,
                  "type": "WORD_READING",
                  "requiredInputs": ["VOICE", "GAZE"],
                  "content": {
                    "readingOrder": "SEQUENTIAL",
                    "words": ["사과", "바나나", "학교", "자동차"]
                  },
                  "answer": {
                    "expectedText": "사과 바나나 학교 자동차"
                  },
                  "analysisTargets": [
                    {"text": "사과"},
                    {"text": "바나나"},
                    {"text": "학교"},
                    {"text": "자동차"}
                  ]
                }
              ]
            }
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TargetTraining target = findTargetTraining();
        if (target == null) {
            return;
        }

        long trainingId = target.voiceTrainingId() == null
                ? target.firstTrainingId()
                : target.voiceTrainingId();
        boolean templateReplaced = target.voiceTrainingId() == null;
        if (templateReplaced) {
            jdbcTemplate.update(
                    "UPDATE trainings SET training_template_id = ? WHERE id = ?",
                    VOICE_TEMPLATE_ID,
                    trainingId
            );
        } else {
            if (target.voiceSequenceNo() != TARGET_SEQUENCE_NO) {
                promoteVoiceTraining(target);
            }
        }
        ensureVoiceTrainingData(trainingId);
    }

    private TargetTraining findTargetTraining() {
        return jdbcTemplate.query(
                """
                SELECT first_training.id AS first_training_id,
                       voice_training.id AS voice_training_id,
                       voice_training.sequence_no AS voice_sequence_no
                  FROM daily_curriculums curriculum
                  JOIN trainings first_training
                    ON first_training.daily_curriculum_id = curriculum.id
                   AND first_training.sequence_no = ?
                  LEFT JOIN trainings voice_training
                    ON voice_training.daily_curriculum_id = curriculum.id
                   AND voice_training.training_template_id = ?
                 WHERE curriculum.student_id = ?
                   AND curriculum.status = 'NOT_STARTED'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM trainings progressed
                        WHERE progressed.daily_curriculum_id = curriculum.id
                          AND (progressed.started_at IS NOT NULL
                               OR progressed.status IN ('IN_PROGRESS', 'COMPLETED'))
                   )
                 ORDER BY curriculum.created_at DESC, curriculum.id DESC
                 LIMIT 1
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    Long voiceTrainingId = resultSet.getObject("voice_training_id", Long.class);
                    Integer voiceSequenceNo = resultSet.getObject("voice_sequence_no", Integer.class);
                    return new TargetTraining(
                            resultSet.getLong("first_training_id"),
                            voiceTrainingId,
                            voiceSequenceNo == null ? 0 : voiceSequenceNo
                    );
                },
                TARGET_SEQUENCE_NO,
                VOICE_TEMPLATE_ID,
                TARGET_STUDENT_ID
        );
    }

    private void promoteVoiceTraining(TargetTraining target) {
        jdbcTemplate.update(
                "UPDATE trainings SET sequence_no = 0 WHERE id = ?",
                target.firstTrainingId()
        );
        jdbcTemplate.update(
                "UPDATE trainings SET sequence_no = ?, status = 'NOT_STARTED' WHERE id = ?",
                TARGET_SEQUENCE_NO,
                target.voiceTrainingId()
        );
        jdbcTemplate.update(
                "UPDATE trainings SET sequence_no = ?, status = 'NOT_READY' WHERE id = ?",
                target.voiceSequenceNo(),
                target.firstTrainingId()
        );
    }

    private void ensureVoiceTrainingData(long trainingId) {
        int updated = jdbcTemplate.update(
                "UPDATE training_datas SET generated_data = ? WHERE train_id = ?",
                VOICE_TRAINING_DATA,
                trainingId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO training_datas (train_id, generated_data, created_at) "
                            + "VALUES (?, ?, CURRENT_TIMESTAMP)",
                    trainingId,
                    VOICE_TRAINING_DATA
            );
        }
    }

    private record TargetTraining(
            long firstTrainingId,
            Long voiceTrainingId,
            int voiceSequenceNo
    ) {
    }
}
