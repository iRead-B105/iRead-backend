INSERT INTO `curriculum_units` (`id`, `unit_name`, `sequence_no`) VALUES
    (1, '음운 인식', 1),
    (2, '읽기 유창성', 2);

INSERT INTO `training_templates`
    (`id`, `curriculum_unit_id`, `name`, `prompt`, `sequence_no`)
VALUES
    (1, 1, '글자 따라 보기',
     '{"trainingType":"VOWEL_TRACE","requiredInputs":["VOICE","GAZE"],"additionalPrompt":"데모 모음 따라 보기","outputTemplate":{"type":"VOWEL_TRACE","data":[{"vowelType":"<string>","target":"<string>","soundText":"<string>","traceAssetKey":"<string>"}]},"supportedFeatureCategories":["GRAPHEME"],"supportedScopes":["CHARACTER"]}',
     1),
    (15, 2, '음절 합쳐 낱말 만들기',
     '{"trainingType":"SYLLABLE_BLEND","requiredInputs":["VOICE"],"additionalPrompt":"데모 음절 합치기","outputTemplate":{"type":"SYLLABLE_BLEND","data":[{"audioParts":["<string>"],"cards":["<string>"],"answerOrder":["<integer>"],"result":"<string>"}]},"supportedFeatureCategories":["SYLLABLE","WORD"],"supportedScopes":["SYLLABLE","WORD"]}',
     1);

INSERT INTO `story_templates` (`id`, `title`, `content`, `image_url`) VALUES
    (1, '별빛 숲의 친구', '별빛이 비치는 숲에서 동물 친구와 함께 길을 찾는 따뜻한 모험', NULL),
    (2, '구름 우체국', '구름 위 우체국에서 잃어버린 편지의 주인을 찾아주는 이야기', NULL);

INSERT INTO `teachers`
    (`id`, `email`, `password`, `name`, `organization`, `created_at`, `gender`, `image_url`)
VALUES
    (1001, 'demo@iread.local',
     '$2a$10$45VbHiaqymHxwX7f4ujvcenYd3sQ.OhFUZYQjKBjbix1N4by6g7r6',
     '데모교사', '아이리드 데모교실', '2026-07-01 09:00:00', NULL, NULL);

INSERT INTO `students`
    (`id`, `teacher_id`, `name`, `birthday`, `gender`, `school`, `guardian`,
     `guardian_contact`, `guardian_email`, `address`, `created_at`, `image_url`, `teacher_memo`)
VALUES
    (2001, 1001, '샛별', '2018-03-15', 'girl', '데모초등학교', NULL,
     NULL, NULL, NULL, '2026-07-01 09:10:00', NULL, '비식별 시연용 학습자');

INSERT INTO `words` (`id`, `content`, `length`) VALUES
    (1, '사과', 2),
    (2, '바나나', 3),
    (3, '학교', 2);

INSERT INTO `daily_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (3001, 2001, 'IN_PROGRESS', '2026-07-28 09:00:00', NULL);

INSERT INTO `trainings`
    (`id`, `training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
VALUES
    (4001, 1, 3001, 1, '2026-07-28 09:00:00', NULL, NULL, 'NOT_STARTED', NULL, NULL),
    (4002, 15, 3001, 2, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL);

INSERT INTO `training_datas` (`id`, `train_id`, `generated_data`, `created_at`) VALUES
    (4101, 4001, '{"schemaVersion":2,"questions":[{"questionNo":1,"type":"VOWEL_TRACE","requiredInputs":["VOICE","GAZE"],"content":{"vowelType":"BASIC","target":"ㅏ","soundText":"ㅏ","traceAssetKey":"vowel_a"},"text":"ㅏ"}]}',
     '2026-07-28 09:00:00');

INSERT INTO `test_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (5001, 2001, 'NOT_STARTED', '2026-07-28 09:00:00', NULL);

INSERT INTO `tests`
    (`id`, `test_curriculum_id`, `training_template_id`, `status`, `result`,
     `accuracy`, `created_at`, `started_at`, `finished_at`, `sequence_no`)
VALUES
    (5101, 5001, 15, 'NOT_STARTED', NULL, NULL, '2026-07-28 09:00:00', NULL, NULL, 1);

INSERT INTO `test_datas` (`id`, `test_id`, `generated_data`, `created_at`) VALUES
    (5201, 5101, '{"questions":[{"questionNumber":1,"text":"학교를 읽어보세요."}]}',
     '2026-07-28 09:00:00');

INSERT INTO `stories`
    (`id`, `student_id`, `story_template_id`, `created_at`, `status`, `progress`)
VALUES
    (6001, 2001, 1, '2026-07-20 10:00:00', 'COMPLETED', 100);

INSERT INTO `story_scenes`
    (`scene_id`, `story_id`, `image_url`, `sequence_no`, `created_at`)
VALUES
    (6101, 6001, NULL, 1, '2026-07-20 10:00:00');

INSERT INTO `story_lines`
    (`id`, `scene_id`, `has_choices`, `content`, `sequence_no`, `created_at`, `read_at`)
VALUES
    (6201, 6101, FALSE, '샛별이는 별빛 숲에서 작은 토끼를 만났어요.', 1,
     '2026-07-20 10:00:00', '2026-07-20 10:01:00');

INSERT INTO `character`
    (`id`, `student_id`, `story_id`, `image_url`, `created_at`, `name`)
VALUES
    (6301, 2001, 6001, NULL, '2026-07-20 10:05:00', '별빛 토끼');
