INSERT INTO `students`
    (`id`, `teacher_id`, `name`, `birthday`, `gender`, `school`, `guardian`,
     `guardian_contact`, `guardian_email`, `address`, `created_at`, `image_url`, `teacher_memo`)
VALUES
    (2002, 1001, '한결', '2018-09-21', 'Boy', '데모초등학교', NULL,
     NULL, NULL, NULL, '2026-07-01 09:20:00', NULL, '비식별 시연용 두 번째 학습자');

INSERT INTO `daily_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (3002, 2002, 'IN_PROGRESS', '2026-07-29 09:00:00', NULL);

INSERT INTO `trainings`
    (`id`, `training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
VALUES
    (4003, 1, 3002, 1, '2026-07-29 09:00:00', '2026-07-29 09:01:00',
     '2026-07-29 09:04:00', 'COMPLETED',
     '{"schemaVersion":2,"questionResults":[{"questionNo":1,"isCorrect":true,"attemptCount":1}]}',
     920),
    (4004, 15, 3002, 2, '2026-07-29 09:00:00', NULL, NULL, 'NOT_STARTED', NULL, NULL);

INSERT INTO `training_datas` (`id`, `train_id`, `generated_data`, `created_at`)
VALUES
    (4103, 4003, '{
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
    }', '2026-07-29 09:00:00'),
    (4104, 4004, '{
      "schemaVersion": 2,
      "questions": [{
        "questionNo": 1,
        "type": "SYLLABLE_BLEND",
        "requiredInputs": ["VOICE"],
        "content": {
          "audioParts": ["나", "무"],
          "cards": ["무", "다", "나"]
        },
        "answer": {
          "answerOrder": [2, 0],
          "result": "나무"
        },
        "analysisTargets": [{
          "path": "$.answer.recordingText",
          "text": "나무",
          "featureCodes": []
        }]
      }]
    }', '2026-07-29 09:00:00');

INSERT INTO `test_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (5002, 2002, 'NOT_STARTED', '2026-07-29 09:00:00', NULL);

INSERT INTO `tests`
    (`id`, `test_curriculum_id`, `training_template_id`, `status`, `result`,
     `accuracy`, `created_at`, `started_at`, `finished_at`, `sequence_no`)
VALUES
    (5102, 5002, 15, 'NOT_STARTED', NULL, NULL, '2026-07-29 09:00:00', NULL, NULL, 1);

INSERT INTO `test_datas` (`id`, `test_id`, `generated_data`, `created_at`)
VALUES
    (5202, 5102, '{"questions":[{"questionNumber":1,"text":"나무를 읽어보세요."}]}',
     '2026-07-29 09:00:00');

INSERT INTO `stories`
    (`id`, `student_id`, `story_template_id`, `created_at`, `status`, `progress`)
VALUES
    (6002, 2002, 2, '2026-07-22 10:00:00', 'COMPLETED', 100);

INSERT INTO `story_scenes`
    (`scene_id`, `story_id`, `image_url`, `sequence_no`, `created_at`)
VALUES
    (6102, 6002, NULL, 1, '2026-07-22 10:00:00');

INSERT INTO `story_lines`
    (`id`, `scene_id`, `has_choices`, `content`, `sequence_no`, `created_at`, `read_at`)
VALUES
    (6202, 6102, FALSE, '한결이는 구름 우체국에서 파란 편지의 주인을 찾았어요.', 1,
     '2026-07-22 10:00:00', '2026-07-22 10:01:00');

INSERT INTO `character`
    (`id`, `student_id`, `story_id`, `image_url`, `created_at`, `name`)
VALUES
    (6302, 2002, 6002, NULL, '2026-07-22 10:05:00', '구름 우체부');
