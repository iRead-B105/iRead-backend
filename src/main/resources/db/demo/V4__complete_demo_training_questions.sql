UPDATE `training_datas`
SET `generated_data` = '{
  "schemaVersion": 2,
  "questions": [{
    "questionNo": 1,
    "type": "VOWEL_TRACE",
    "requiredInputs": ["VOICE", "GAZE"],
    "content": {
      "vowelType": "BASIC",
      "target": "ㅏ",
      "soundText": "ㅏ",
      "traceAssetKey": "vowel_a"
    },
    "answer": {"target": "ㅏ"},
    "analysisTargets": [{
      "path": "$.content.target",
      "text": "ㅏ",
      "featureCodes": []
    }],
    "text": "ㅏ"
  }]
}'
WHERE `id` = 4101;

INSERT INTO `training_datas` (`id`, `train_id`, `generated_data`, `created_at`)
VALUES (
  4102,
  4002,
  '{
    "schemaVersion": 2,
    "questions": [{
      "questionNo": 1,
      "type": "SYLLABLE_BLEND",
      "requiredInputs": ["VOICE"],
      "content": {
        "audioParts": ["사", "과"],
        "cards": ["과", "나", "사"]
      },
      "answer": {
        "answerOrder": [2, 0],
        "result": "사과"
      },
      "analysisTargets": [{
        "path": "$.answer.recordingText",
        "text": "사과",
        "featureCodes": []
      }]
    }]
  }',
  '2026-07-28 09:00:00'
)
ON DUPLICATE KEY UPDATE
  `generated_data` = VALUES(`generated_data`);
