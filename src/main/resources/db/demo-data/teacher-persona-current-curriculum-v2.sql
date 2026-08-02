-- 현재 훈련이 4개뿐인 교사용 데모 커리큘럼을 승인된 5개 구성으로 보정한다.
-- 운영 기준 데이터와 분리된 데모 전용 보정이며, 반복 실행해도 기존 이력은 변경하지 않는다.

CREATE TEMPORARY TABLE demo_current_curriculum_corrections (
    persona_no INT PRIMARY KEY,
    curriculum_id BIGINT NOT NULL,
    training_id BIGINT NOT NULL,
    training_data_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    training_type VARCHAR(80) NOT NULL,
    required_inputs JSON NOT NULL,
    content JSON NOT NULL,
    analysis_targets JSON NOT NULL,
    answer JSON NOT NULL
);

INSERT INTO demo_current_curriculum_corrections
    (persona_no, curriculum_id, training_id, training_data_id, template_id,
     training_type, required_inputs, content, analysis_targets, answer)
VALUES
    (2, 120023, 130213, 135213, 18, 'DOUBLE_FINAL_BUILD', JSON_ARRAY('VOICE'),
     JSON_OBJECT('targetAudioText', '닭', 'initialChoices', JSON_ARRAY('ㄷ', 'ㅂ', 'ㄱ'),
                 'medialChoices', JSON_ARRAY('ㅏ', 'ㅓ', 'ㅗ'),
                 'finalChoices', JSON_ARRAY('ㄺ', 'ㄱ', 'ㄴ'),
                 'initialAnswerIndex', 0, 'medialAnswerIndex', 0,
                 'finalAnswerIndex', 0, 'result', '닭'),
     JSON_ARRAY(JSON_OBJECT('text', '닭')), JSON_OBJECT('expectedText', '닭')),
    (3, 120033, 130313, 135313, 21, 'SYLLABLE_REPLACE', JSON_ARRAY('VOICE'),
     JSON_OBJECT('source', '사과', 'targetAudioText', '사자', 'replaceIndex', 1,
                 'choices', JSON_ARRAY('자', '과', '나'), 'answerIndex', 0, 'result', '사자'),
     JSON_ARRAY(JSON_OBJECT('text', '사자')), JSON_OBJECT('expectedText', '사자')),
    (5, 120053, 130513, 135513, 27, 'SENTENCE_ASSEMBLY', JSON_ARRAY('VOICE', 'GAZE'),
     JSON_OBJECT('cards', JSON_ARRAY('책을', '나는', '읽어요.'),
                 'answerOrder', JSON_ARRAY(1, 0, 2), 'completedSentence', '나는 책을 읽어요.'),
     JSON_ARRAY(JSON_OBJECT('text', '나는 책을 읽어요.')),
     JSON_OBJECT('expectedText', '나는 책을 읽어요.')),
    (6, 120063, 130613, 135613, 30, 'SENTENCE_REPEAT', JSON_ARRAY('VOICE', 'GAZE'),
     JSON_OBJECT('sentence', '친구와 천천히 책을 읽어요.', 'emotion', 'CALM'),
     JSON_ARRAY(JSON_OBJECT('text', '친구와 천천히 책을 읽어요.')),
     JSON_OBJECT('expectedText', '친구와 천천히 책을 읽어요.')),
    (7, 120073, 130713, 135713, 33, 'REPEATED_SENTENCE_READING', JSON_ARRAY('VOICE', 'GAZE'),
     JSON_OBJECT('sentence', '오늘도 또박또박 읽어요.', 'repeatCount', 2),
     JSON_ARRAY(JSON_OBJECT('text', '오늘도 또박또박 읽어요.')),
     JSON_OBJECT('expectedText', '오늘도 또박또박 읽어요.')),
    (8, 120083, 130813, 135813, 2, 'CONSONANT_TRACE', JSON_ARRAY('VOICE', 'GAZE'),
     JSON_OBJECT('consonantType', 'BASIC', 'target', 'ㄱ', 'soundText', 'ㄱ',
                 'traceAssetKey', 'consonant_giyeok'),
     JSON_ARRAY(JSON_OBJECT('text', 'ㄱ')), JSON_OBJECT('expectedText', 'ㄱ')),
    (9, 120093, 130913, 135913, 5, 'VOWEL_SOUND_CHOICE', JSON_ARRAY(),
     JSON_OBJECT('audioText', 'ㅏ', 'choices', JSON_ARRAY('ㅏ', 'ㅓ', 'ㅗ'), 'answerIndex', 0),
     JSON_ARRAY(JSON_OBJECT('text', 'ㅏ')), JSON_OBJECT('answerIndex', 0)) ,
    (10, 120103, 131013, 136013, 8, 'WORD_INITIAL_CHOICE', JSON_ARRAY(),
     JSON_OBJECT('audioText', '사과', 'choices', JSON_ARRAY('ㅅ', 'ㅈ', 'ㄱ'), 'answerIndex', 0),
     JSON_ARRAY(JSON_OBJECT('text', '사과')), JSON_OBJECT('answerIndex', 0)),
    (11, 120113, 131113, 136113, 11, 'WORD_FINAL_SOUND_CHOICE', JSON_ARRAY(),
     JSON_OBJECT('audioText', '산', 'choices', JSON_ARRAY('ㄴ', 'ㄹ', 'ㅁ'), 'answerIndex', 0),
     JSON_ARRAY(JSON_OBJECT('text', '산')), JSON_OBJECT('answerIndex', 0)),
    (12, 120123, 131213, 136213, 14, 'PHONEME_BLEND', JSON_ARRAY('VOICE'),
     JSON_OBJECT('audioParts', JSON_ARRAY('ㄱ', 'ㅏ'), 'cards', JSON_ARRAY('ㄱ', 'ㅏ', 'ㄴ'),
                 'answerOrder', JSON_ARRAY(0, 1), 'result', '가'),
     JSON_ARRAY(JSON_OBJECT('text', '가')), JSON_OBJECT('expectedText', '가'));

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    correction.training_id,
    correction.template_id,
    correction.curriculum_id,
    5,
    TIMESTAMPADD(MINUTE, 5, curriculum.created_at),
    NULL,
    NULL,
    'NOT_READY',
    NULL,
    NULL
FROM demo_current_curriculum_corrections correction
JOIN daily_curriculums curriculum ON curriculum.id = correction.curriculum_id
JOIN training_templates template ON template.id = correction.template_id
WHERE curriculum.status = 'NOT_STARTED'
  AND (SELECT COUNT(*) FROM trainings existing
       WHERE existing.daily_curriculum_id = correction.curriculum_id) = 4
  AND NOT EXISTS (
      SELECT 1 FROM trainings existing
      WHERE existing.id = correction.training_id
         OR (existing.daily_curriculum_id = correction.curriculum_id
             AND existing.sequence_no = 5)
  );

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    correction.training_data_id,
    correction.training_id,
    JSON_OBJECT(
        'schemaVersion', 2,
        'trainingType', correction.training_type,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionId', CONCAT('persona-', correction.persona_no, '-13-1'),
                'questionNo', 1,
                'type', correction.training_type,
                'requiredInputs', correction.required_inputs,
                'content', correction.content,
                'analysisTargets', correction.analysis_targets,
                'answer', correction.answer
            ),
            JSON_OBJECT(
                'questionId', CONCAT('persona-', correction.persona_no, '-13-2'),
                'questionNo', 2,
                'type', correction.training_type,
                'requiredInputs', correction.required_inputs,
                'content', correction.content,
                'analysisTargets', correction.analysis_targets,
                'answer', correction.answer
            ),
            JSON_OBJECT(
                'questionId', CONCAT('persona-', correction.persona_no, '-13-3'),
                'questionNo', 3,
                'type', correction.training_type,
                'requiredInputs', correction.required_inputs,
                'content', correction.content,
                'analysisTargets', correction.analysis_targets,
                'answer', correction.answer
            )
        )
    ),
    TIMESTAMPADD(MINUTE, 1, training.created_at)
FROM demo_current_curriculum_corrections correction
JOIN trainings training ON training.id = correction.training_id
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = correction.training_data_id
       OR existing.train_id = correction.training_id
);

DROP TEMPORARY TABLE demo_current_curriculum_corrections;
