-- Demo daily curricula mirror the production planner's five-training policy.
-- Templates referenced here must exist before the demo rows are inserted because
-- Flyway runs before TrainingTemplateDataInitializer on a fresh database.
INSERT INTO `curriculum_units` (`id`, `unit_name`, `sequence_no`)
VALUES
    (4, '글자 자르기', 4),
    (7, '문장 완성 및 이해', 7),
    (8, '유창하게 읽기', 8)
ON DUPLICATE KEY UPDATE
    `id` = VALUES(`id`);

INSERT INTO `training_templates`
    (`id`, `curriculum_unit_id`, `name`, `prompt`, `sequence_no`)
VALUES
    (20, 4, '음절 빼기',
     '{"trainingType":"SYLLABLE_DELETE","requiredInputs":["VOICE"],"additionalPrompt":"두 음절 이상의 source에서 한 음절을 제거해 목표 낱말을 만든다.","outputTemplate":{"type":"SYLLABLE_DELETE","data":[{"source":"<string>","targetAudioText":"<string>","syllables":["<string>"],"deleteIndex":"<integer>","result":"<string>"}]},"supportedFeatureCategories":["SYLLABLE","WORD"],"supportedScopes":["SYLLABLE","WORD"]}',
     2),
    (29, 7, '그림과 문장 연결하기',
     '{"trainingType":"IMAGE_SENTENCE_MATCH","requiredInputs":["VOICE","GAZE"],"additionalPrompt":"그림의 인물, 행동, 장소와 물체가 명확한 장면을 만들고 알맞은 문장을 고르게 한다.","outputTemplate":{"type":"IMAGE_SENTENCE_MATCH","data":[{"imagePrompt":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["MORPH","WORD","SENTENCE"],"supportedScopes":["WORD","SENTENCE"]}',
     3),
    (31, 8, '단어 이어 읽기',
     '{"trainingType":"WORD_CHAIN_READING","requiredInputs":["VOICE","GAZE"],"additionalPrompt":"여러 낱말을 정해진 순서로 자연스럽게 이어 읽는다.","outputTemplate":{"type":"WORD_CHAIN_READING","data":[{"words":["<string>"],"requiredOrder":"<string>"}]},"supportedFeatureCategories":["GRAPHEME","SYLLABLE","PHONOLOGY","WORD"],"supportedScopes":["SYLLABLE","WORD","WORD_BOUNDARY"]}',
     2)
ON DUPLICATE KEY UPDATE
    `id` = VALUES(`id`);

-- The first learner starts with a previously unseen image-and-sentence activity.
UPDATE `trainings`
SET `training_template_id` = 29,
    `sequence_no` = 1,
    `status` = 'NOT_STARTED',
    `started_at` = NULL,
    `finished_at` = NULL,
    `result` = NULL,
    `accuracy` = NULL
WHERE `id` = 4001;

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 1, 3001, 3, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3001 AND `sequence_no` = 3
);

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 20, 3001, 4, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3001 AND `sequence_no` = 4
);

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 31, 3001, 5, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3001 AND `sequence_no` = 5
);

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 20, 3002, 3, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3002 AND `sequence_no` = 3
);

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 29, 3002, 4, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3002 AND `sequence_no` = 4
);

INSERT INTO `trainings`
    (`training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT 31, 3002, 5, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `trainings` WHERE `daily_curriculum_id` = 3002 AND `sequence_no` = 5
);

UPDATE `training_datas`
SET `generated_data` = '{
  "schemaVersion": 2,
  "questions": [{
    "questionNo": 1,
    "type": "IMAGE_SENTENCE_MATCH",
    "requiredInputs": ["VOICE", "GAZE"],
    "content": {
      "imagePrompt": "노란 우산을 쓴 아이가 비 오는 학교 앞을 걷는 따뜻한 동화 장면",
      "choices": [
        "아이가 노란 우산을 쓰고 학교 앞을 걸어요.",
        "아이가 운동장에서 공을 차요.",
        "아이가 방에서 책을 읽어요."
      ]
    },
    "answer": {
      "answerIndex": 0,
      "completedSentence": "아이가 노란 우산을 쓰고 학교 앞을 걸어요."
    },
    "analysisTargets": [{
      "path": "$.answer.completedSentence",
      "text": "아이가 노란 우산을 쓰고 학교 앞을 걸어요.",
      "featureCodes": []
    }],
    "text": "그림에 알맞은 문장을 골라 보세요."
  }]
}'
WHERE `train_id` = 4001;

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "VOWEL_TRACE",
        "requiredInputs": ["VOICE", "GAZE"],
        "content": {
          "vowelType": "BASIC",
          "target": "ㅓ",
          "soundText": "ㅓ",
          "traceAssetKey": "vowel_eo"
        },
        "answer": {"target": "ㅓ"},
        "analysisTargets": [{
          "path": "$.content.target",
          "text": "ㅓ",
          "featureCodes": []
        }],
        "text": "ㅓ"
      }]
    }', '2026-07-28 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3001
  AND training.`sequence_no` = 3
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "SYLLABLE_DELETE",
        "requiredInputs": ["VOICE"],
        "content": {
          "source": "토마토",
          "targetAudioText": "토토",
          "syllables": ["토", "마", "토"]
        },
        "answer": {
          "deleteIndex": 1,
          "result": "토토"
        },
        "analysisTargets": [{
          "path": "$.answer.result",
          "text": "토토",
          "featureCodes": []
        }]
      }]
    }', '2026-07-28 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3001
  AND training.`sequence_no` = 4
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "WORD_CHAIN_READING",
        "requiredInputs": ["VOICE", "GAZE"],
        "content": {
          "words": ["나무", "하늘", "바다", "구름"],
          "requiredOrder": "SEQUENTIAL"
        },
        "answer": {
          "expectedText": "나무 하늘 바다 구름"
        },
        "analysisTargets": [{
          "path": "$.answer.expectedText",
          "text": "나무 하늘 바다 구름",
          "featureCodes": []
        }]
      }]
    }', '2026-07-28 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3001
  AND training.`sequence_no` = 5
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "SYLLABLE_DELETE",
        "requiredInputs": ["VOICE"],
        "content": {
          "source": "강아지",
          "targetAudioText": "강지",
          "syllables": ["강", "아", "지"]
        },
        "answer": {
          "deleteIndex": 1,
          "result": "강지"
        },
        "analysisTargets": [{
          "path": "$.answer.result",
          "text": "강지",
          "featureCodes": []
        }]
      }]
    }', '2026-07-29 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3002
  AND training.`sequence_no` = 3
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "IMAGE_SENTENCE_MATCH",
        "requiredInputs": ["VOICE", "GAZE"],
        "content": {
          "imagePrompt": "공원에서 빨간 공을 던지는 아이와 강아지가 함께 있는 밝은 동화 장면",
          "choices": [
            "아이가 공원에서 강아지와 공놀이를 해요.",
            "아이가 교실에서 그림을 그려요.",
            "아이가 주방에서 빵을 만들어요."
          ]
        },
        "answer": {
          "answerIndex": 0,
          "completedSentence": "아이가 공원에서 강아지와 공놀이를 해요."
        },
        "analysisTargets": [{
          "path": "$.answer.completedSentence",
          "text": "아이가 공원에서 강아지와 공놀이를 해요.",
          "featureCodes": []
        }]
      }]
    }', '2026-07-29 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3002
  AND training.`sequence_no` = 4
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );

INSERT INTO `training_datas` (`train_id`, `generated_data`, `created_at`)
SELECT training.id, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "WORD_CHAIN_READING",
        "requiredInputs": ["VOICE", "GAZE"],
        "content": {
          "words": ["사과", "기차", "토끼", "학교"],
          "requiredOrder": "SEQUENTIAL"
        },
        "answer": {
          "expectedText": "사과 기차 토끼 학교"
        },
        "analysisTargets": [{
          "path": "$.answer.expectedText",
          "text": "사과 기차 토끼 학교",
          "featureCodes": []
        }]
      }]
    }', '2026-07-29 09:00:00'
FROM `trainings` training
WHERE training.`daily_curriculum_id` = 3002
  AND training.`sequence_no` = 5
  AND NOT EXISTS (
      SELECT 1 FROM `training_datas` data WHERE data.`train_id` = training.`id`
  );
