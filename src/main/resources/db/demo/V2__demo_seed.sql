INSERT INTO `curriculum_units` (`id`, `unit_name`, `sequence_no`) VALUES
    (1, '음운 인식', 1),
    (2, '소리 듣고 고르기', 2),
    (3, '글자 만들기', 3);

INSERT INTO `training_templates`
    (`id`, `curriculum_unit_id`, `name`, `prompt`, `sequence_no`)
VALUES
    (1, 1, '글자 따라 보기',
     '{"trainingType":"VOWEL_TRACE","requiredInputs":["VOICE","GAZE"],"additionalPrompt":"데모 모음 따라 보기","outputTemplate":{"type":"VOWEL_TRACE","data":[{"vowelType":"<string>","target":"<string>","soundText":"<string>","traceAssetKey":"<string>"}]},"supportedFeatureCategories":["GRAPHEME"],"supportedScopes":["CHARACTER"]}',
     1),
    (4, 2, '자음 소리 고르기',
     '{"trainingType":"CONSONANT_SOUND_CHOICE","requiredInputs":[],"additionalPrompt":"자음 소리를 듣고 알맞은 자음 카드를 고른다. audioText는 자음 이름이 아닌 실제 소릿값이고 choices에는 한글 자음만 넣는다. 정답과 혼동 가능한 오답을 포함하고 answerIndex를 실제 정답 위치와 일치시킨다.","outputTemplate":{"type":"CONSONANT_SOUND_CHOICE","data":[{"audioText":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["GRAPHEME"],"supportedScopes":["CHARACTER"]}',
     1),
    (5, 2, '모음 소리 고르기',
     '{"trainingType":"VOWEL_SOUND_CHOICE","requiredInputs":[],"additionalPrompt":"모음 소리를 듣고 알맞은 모음 카드를 고른다. audioText는 목표 모음의 실제 소릿값이고 choices에는 한글 모음만 넣는다. 형태나 소리가 비슷한 오답을 포함한다.","outputTemplate":{"type":"VOWEL_SOUND_CHOICE","data":[{"audioText":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["GRAPHEME"],"supportedScopes":["CHARACTER"]}',
     2),
    (7, 2, '음절의 첫소리 찾기',
     '{"trainingType":"SYLLABLE_INITIAL_CHOICE","requiredInputs":[],"additionalPrompt":"한글 음절 하나를 듣고 실제 초성을 고른다. choices에는 초성 자음만 넣고 오답은 정답과 형태나 소리가 비슷한 자음을 우선 사용한다.","outputTemplate":{"type":"SYLLABLE_INITIAL_CHOICE","data":[{"audioText":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["GRAPHEME","SYLLABLE"],"supportedScopes":["CHARACTER","SYLLABLE"]}',
     4),
    (8, 2, '낱말의 첫소리 찾기',
     '{"trainingType":"WORD_INITIAL_CHOICE","requiredInputs":[],"additionalPrompt":"친숙한 실제 낱말을 듣고 첫 음절의 초성을 고른다. choices에는 초성 자음만 넣고 받침이나 의미가 아니라 첫소리만으로 판단할 수 있게 구성한다.","outputTemplate":{"type":"WORD_INITIAL_CHOICE","data":[{"audioText":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["GRAPHEME","WORD"],"supportedScopes":["CHARACTER","WORD"]}',
     5),
    (11, 2, '낱말의 끝소리 고르기',
     '{"trainingType":"WORD_FINAL_SOUND_CHOICE","requiredInputs":[],"additionalPrompt":"친숙한 받침 포함 낱말을 듣고 마지막에 들리는 표준 발음의 끝소리를 고른다. 낮은 난이도에서는 음운 변동이 없는 낱말을 우선한다.","outputTemplate":{"type":"WORD_FINAL_SOUND_CHOICE","data":[{"audioText":"<string>","choices":["<string>"],"answerIndex":"<integer>"}]},"supportedFeatureCategories":["GRAPHEME","PHONOLOGY","WORD"],"supportedScopes":["CHARACTER","SYLLABLE","WORD"]}',
     8),
    (15, 3, '음절 합쳐 낱말 만들기',
     '{"trainingType":"SYLLABLE_BLEND","questionType":"SENTENCE_READING","requiredInputs":["VOICE"],"additionalPrompt":"데모 음절 합치기","outputTemplate":{"type":"SYLLABLE_BLEND","data":[{"audioParts":["<string>"],"cards":["<string>"],"answerOrder":["<integer>"],"result":"<string>"}]},"supportedFeatureCategories":["SYLLABLE","WORD"],"supportedScopes":["SYLLABLE","WORD"]}',
     2);

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

-- 샛별의 이야기는 이야기 세션 API와 AI mock을 통해 생성하므로
-- 완성된 이야기를 미리 넣지 않고 이야기 템플릿만 제공한다.

-- Expanded, non-identifying teacher demo data.
-- IDs use dedicated ranges so the base and expanded demo scenarios can coexist.
-- This migration owns templates 1 and 15. The application initializer adds the remaining catalog after Flyway.

INSERT INTO `students`
    (`id`, `teacher_id`, `name`, `birthday`, `gender`, `school`, `guardian`,
     `guardian_contact`, `guardian_email`, `address`, `created_at`, `image_url`, `teacher_memo`)
VALUES
    (2101, 1001, '김하늘', '2018-05-12', 'Girl', '새봄초등학교', '김보호', '010-0000-2101',
     'guardian2101@example.invalid', '서울시 데모구', '2026-05-01 09:00:00', NULL,
     '최근 읽기 속도와 정확도가 함께 향상되고 있습니다.'),
    (2102, 1001, '이도윤', '2017-11-03', 'Boy', '푸른초등학교', '이보호', '010-0000-2102',
     NULL, NULL, '2026-07-22 09:00:00', NULL, '신규 등록 아동으로 초기 검사가 필요합니다.'),
    (2103, 1001, '박서아', '2019-02-18', 'Girl', '한결초등학교', '박보호', '010-0000-2103',
     NULL, NULL, '2026-06-01 09:00:00', NULL, '문장 이해 영역의 반복 훈련을 권장합니다.'),
    (2104, 1001, '최우진', '2016-09-27', 'Boy', '새봄초등학교', '최보호', '010-0000-2104',
     NULL, NULL, '2026-05-15 09:00:00', NULL, '정확도가 높아 심화 읽기 활동을 진행 중입니다.'),
    (2105, 1001, '정민준', '2015-04-09', 'Boy', '가온초등학교', '정보호', '010-0000-2105',
     NULL, NULL, '2026-04-01 09:00:00', NULL, '최근 학습 공백이 있어 참여 확인이 필요합니다.'),
    (2106, 1001, '윤서준', '2014-12-21', 'Boy', '푸른초등학교', '윤보호', '010-0000-2106',
     NULL, NULL, '2026-04-10 09:00:00', NULL, NULL),
    (2107, 1001, '한지민', '2020-01-30', 'Girl', '해봄초등학교', '한보호', '010-0000-2107',
     NULL, NULL, '2026-07-01 09:00:00', NULL, '첫소리 구별 활동에 흥미를 보입니다.'),
    (2108, 1001, '강유진', '2018-08-14', 'Girl', '가온초등학교', '강보호', '010-0000-2108',
     NULL, NULL, '2026-06-12 09:00:00', NULL, NULL),
    (2109, 1001, '오시우', '2017-06-06', 'Boy', '한결초등학교', '오보호', '010-0000-2109',
     NULL, NULL, '2026-06-20 09:00:00', NULL, '받침이 포함된 낱말에서 재시도가 잦습니다.'),
    (2110, 1001, '송예린', '2016-03-25', 'Girl', '해봄초등학교', '송보호', '010-0000-2110',
     NULL, NULL, '2026-05-20 09:00:00', NULL, NULL),
    (2111, 1001, '임도현', '2015-10-17', 'Boy', '새봄초등학교', '임보호', '010-0000-2111',
     NULL, NULL, '2026-05-05 09:00:00', NULL, '긴 문장에서는 끊어 읽기 안내가 필요합니다.');

INSERT INTO `daily_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (3201, 2101, 'COMPLETED', '2026-07-05 09:00:00', '2026-07-05 09:15:00'),
    (3202, 2101, 'COMPLETED', '2026-07-24 09:00:00', '2026-07-24 09:14:00'),
    (3203, 2101, 'NOT_STARTED', '2026-07-28 09:00:00', NULL),
    (3204, 2103, 'COMPLETED', '2026-07-10 10:00:00', '2026-07-10 10:18:00'),
    (3205, 2103, 'COMPLETED', '2026-07-27 10:00:00', '2026-07-27 10:16:00'),
    (3206, 2104, 'COMPLETED', '2026-07-18 11:00:00', '2026-07-18 11:12:00'),
    (3207, 2105, 'COMPLETED', '2026-06-30 14:00:00', '2026-06-30 14:20:00'),
    (3208, 2106, 'COMPLETED', '2026-07-25 13:00:00', '2026-07-25 13:19:00'),
    (3209, 2107, 'COMPLETED', '2026-07-24 15:00:00', '2026-07-24 15:11:00'),
    (3210, 2108, 'COMPLETED', '2026-07-22 09:30:00', '2026-07-22 09:45:00'),
    (3211, 2109, 'COMPLETED', '2026-07-21 10:30:00', '2026-07-21 10:50:00'),
    (3212, 2110, 'COMPLETED', '2026-07-19 11:30:00', '2026-07-19 11:42:00'),
    (3213, 2111, 'COMPLETED', '2026-07-17 14:30:00', '2026-07-17 14:52:00');

INSERT INTO `trainings`
    (`id`, `training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
VALUES
    (4201, 15, 3201, 1, '2026-07-05 09:00:00', '2026-07-05 09:01:00', '2026-07-05 09:07:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"낱말을 읽어 보세요.","isCorrect":true,"correctAnswer":"사과","selectedAnswer":"사과"}],"retryCount":1}', 700),
    (4202, 15, 3201, 2, '2026-07-05 09:00:00', '2026-07-05 09:08:00', '2026-07-05 09:15:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"문장을 읽어 보세요.","isCorrect":false,"correctAnswer":"하늘이 맑습니다.","selectedAnswer":"하늘이 말습니다."}],"retryCount":2}', 680),
    (4203, 15, 3202, 1, '2026-07-24 09:00:00', '2026-07-24 09:01:00', '2026-07-24 09:07:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"낱말을 읽어 보세요.","isCorrect":true,"correctAnswer":"바나나","selectedAnswer":"바나나"}],"retryCount":0}', 860),
    (4204, 15, 3202, 2, '2026-07-24 09:00:00', '2026-07-24 09:08:00', '2026-07-24 09:14:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"문장을 읽어 보세요.","isCorrect":true,"correctAnswer":"친구와 공원에 갑니다.","selectedAnswer":"친구와 공원에 갑니다."}],"retryCount":0}', 900),
    (4205, 1, 3203, 1, '2026-07-28 09:00:00', NULL, NULL, 'NOT_STARTED', NULL, NULL),
    (4206, 15, 3203, 2, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL),
    (4207, 15, 3203, 3, '2026-07-28 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL),
    (4208, 15, 3204, 1, '2026-07-10 10:00:00', '2026-07-10 10:01:00', '2026-07-10 10:09:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"문장을 읽어 보세요.","isCorrect":false,"correctAnswer":"토끼가 산책을 합니다.","selectedAnswer":"토끼가 산책을 함니다."}],"retryCount":2}', 620),
    (4209, 15, 3204, 2, '2026-07-10 10:00:00', '2026-07-10 10:10:00', '2026-07-10 10:18:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"빈칸을 채우세요.","isCorrect":true,"correctAnswer":"학교","selectedAnswer":"학교"}],"retryCount":1}', 720),
    (4210, 15, 3205, 1, '2026-07-27 10:00:00', '2026-07-27 10:01:00', '2026-07-27 10:08:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"문장을 읽어 보세요.","isCorrect":true,"correctAnswer":"나비가 꽃에 앉았습니다.","selectedAnswer":"나비가 꽃에 앉았습니다."}],"retryCount":1}', 760),
    (4211, 15, 3205, 2, '2026-07-27 10:00:00', '2026-07-27 10:09:00', '2026-07-27 10:16:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"question":"알맞은 문장을 고르세요.","isCorrect":false,"correctAnswer":"아이가 책을 읽습니다.","selectedAnswer":"아이가 공을 찹니다."}],"retryCount":2}', 650),
    (4212, 15, 3206, 1, '2026-07-18 11:00:00', '2026-07-18 11:01:00', '2026-07-18 11:06:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 940),
    (4213, 15, 3206, 2, '2026-07-18 11:00:00', '2026-07-18 11:07:00', '2026-07-18 11:12:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 920),
    (4214, 15, 3207, 1, '2026-06-30 14:00:00', '2026-06-30 14:01:00', '2026-06-30 14:10:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":3}', 560),
    (4215, 15, 3207, 2, '2026-06-30 14:00:00', '2026-06-30 14:11:00', '2026-06-30 14:20:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":3}', 520),
    (4216, 15, 3208, 1, '2026-07-25 13:00:00', '2026-07-25 13:01:00', '2026-07-25 13:10:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":1}', 780),
    (4217, 15, 3208, 2, '2026-07-25 13:00:00', '2026-07-25 13:11:00', '2026-07-25 13:19:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":2}', 690),
    (4218, 1, 3209, 1, '2026-07-24 15:00:00', '2026-07-24 15:01:00', '2026-07-24 15:05:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 880),
    (4219, 1, 3209, 2, '2026-07-24 15:00:00', '2026-07-24 15:06:00', '2026-07-24 15:11:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":1}', 840),
    (4220, 15, 3210, 1, '2026-07-22 09:30:00', '2026-07-22 09:31:00', '2026-07-22 09:38:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 910),
    (4221, 15, 3210, 2, '2026-07-22 09:30:00', '2026-07-22 09:39:00', '2026-07-22 09:45:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 930),
    (4222, 1, 3211, 1, '2026-07-21 10:30:00', '2026-07-21 10:31:00', '2026-07-21 10:40:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":3}', 480),
    (4223, 1, 3211, 2, '2026-07-21 10:30:00', '2026-07-21 10:41:00', '2026-07-21 10:50:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":3}', 510),
    (4224, 15, 3212, 1, '2026-07-19 11:30:00', '2026-07-19 11:31:00', '2026-07-19 11:36:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 870),
    (4225, 15, 3212, 2, '2026-07-19 11:30:00', '2026-07-19 11:37:00', '2026-07-19 11:42:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":0}', 890),
    (4226, 15, 3213, 1, '2026-07-17 14:30:00', '2026-07-17 14:31:00', '2026-07-17 14:42:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":false}],"retryCount":2}', 660),
    (4227, 15, 3213, 2, '2026-07-17 14:30:00', '2026-07-17 14:43:00', '2026-07-17 14:52:00',
     'COMPLETED', '{"questions":[{"questionNumber":1,"isCorrect":true}],"retryCount":1}', 740);

INSERT INTO `test_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (5401, 2101, 'COMPLETED', '2026-05-30 10:00:00', '2026-05-30 10:10:00'),
    (5402, 2101, 'COMPLETED', '2026-06-28 10:00:00', '2026-06-28 10:09:00'),
    (5403, 2101, 'COMPLETED', '2026-07-24 10:00:00', '2026-07-24 10:08:00'),
    (5404, 2103, 'COMPLETED', '2026-06-12 11:00:00', '2026-06-12 11:12:00'),
    (5405, 2103, 'COMPLETED', '2026-07-10 11:00:00', '2026-07-10 11:10:00'),
    (5406, 2104, 'COMPLETED', '2026-07-18 12:00:00', '2026-07-18 12:08:00'),
    (5407, 2105, 'COMPLETED', '2026-06-30 15:00:00', '2026-06-30 15:15:00'),
    (5408, 2109, 'COMPLETED', '2026-07-21 11:00:00', '2026-07-21 11:14:00');

INSERT INTO `tests`
    (`id`, `test_curriculum_id`, `training_template_id`, `status`, `result`,
     `accuracy`, `created_at`, `started_at`, `finished_at`, `sequence_no`)
VALUES
    (5501, 5401, 15, 'COMPLETED', '{"overallScore":70,"questions":[{"questionNumber":1,"isCorrect":true}],"readingTimeSeconds":132,"solvingTimeSeconds":210}', 70,
     '2026-05-30 10:00:00', '2026-05-30 10:01:00', '2026-05-30 10:10:00', 1),
    (5502, 5402, 15, 'COMPLETED', '{"overallScore":76,"questions":[{"questionNumber":1,"isCorrect":true}],"readingTimeSeconds":110,"solvingTimeSeconds":185}', 78,
     '2026-06-28 10:00:00', '2026-06-28 10:01:00', '2026-06-28 10:09:00', 1),
    (5503, 5403, 15, 'COMPLETED', '{"overallScore":84,"questions":[{"questionNumber":1,"isCorrect":true},{"questionNumber":2,"isCorrect":false}],"readingTimeSeconds":92,"solvingTimeSeconds":168}', 86,
     '2026-07-24 10:00:00', '2026-07-24 10:01:00', '2026-07-24 10:08:00', 1),
    (5504, 5404, 15, 'COMPLETED', '{"overallScore":68,"questions":[{"questionNumber":1,"isCorrect":false}],"readingTimeSeconds":145,"solvingTimeSeconds":230}', 69,
     '2026-06-12 11:00:00', '2026-06-12 11:01:00', '2026-06-12 11:12:00', 1),
    (5505, 5405, 15, 'COMPLETED', '{"overallScore":74,"questions":[{"questionNumber":1,"isCorrect":true}],"readingTimeSeconds":125,"solvingTimeSeconds":205}', 75,
     '2026-07-10 11:00:00', '2026-07-10 11:01:00', '2026-07-10 11:10:00', 1),
    (5506, 5406, 15, 'COMPLETED', '{"overallScore":91,"questions":[{"questionNumber":1,"isCorrect":true}],"readingTimeSeconds":80,"solvingTimeSeconds":140}', 92,
     '2026-07-18 12:00:00', '2026-07-18 12:01:00', '2026-07-18 12:08:00', 1),
    (5507, 5407, 15, 'COMPLETED', '{"overallScore":55,"questions":[],"readingTimeSeconds":170,"solvingTimeSeconds":260}', 56,
     '2026-06-30 15:00:00', '2026-06-30 15:01:00', '2026-06-30 15:15:00', 1),
    (5508, 5408, 15, 'COMPLETED', '{"overallScore":49,"questions":[{"questionNumber":1,"isCorrect":false}],"readingTimeSeconds":180,"solvingTimeSeconds":275}', 50,
     '2026-07-21 11:00:00', '2026-07-21 11:01:00', '2026-07-21 11:14:00', 1);

INSERT INTO `gaze_sessions`
    (`id`, `student_id`, `test_id`, `training_id`, `story_id`, `content_type`,
     `started_at`, `ended_at`, `data_url`, `status`, `calibration_status`, `created_at`)
VALUES
    (7201, 2101, NULL, 4201, NULL, 'TRAINING', '2026-07-05 09:01:00', '2026-07-05 09:07:00',
     '/gaze/2101/gaze-7201-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-07-05 09:01:00'),
    (7202, 2101, NULL, 4203, NULL, 'TRAINING', '2026-07-24 09:01:00', '2026-07-24 09:07:00',
     '/gaze/2101/gaze-7202-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-07-24 09:01:00'),
    (7203, 2103, NULL, 4210, NULL, 'TRAINING', '2026-07-27 10:01:00', '2026-07-27 10:08:00',
     '/gaze/2103/gaze-7203-00000000-0000-0000-0000-000000000000.json', 'FAILED', 'SUCCESS', '2026-07-27 10:01:00'),
    (7204, 2104, NULL, 4212, NULL, 'TRAINING', '2026-07-18 11:01:00', '2026-07-18 11:06:00',
     '/gaze/2104/gaze-7204-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-07-18 11:01:00'),
    (7211, 2101, 5501, NULL, NULL, 'TEST', '2026-05-30 10:01:00', '2026-05-30 10:10:00',
     '/gaze/2101/gaze-7211-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-05-30 10:01:00'),
    (7212, 2101, 5502, NULL, NULL, 'TEST', '2026-06-28 10:01:00', '2026-06-28 10:09:00',
     '/gaze/2101/gaze-7212-00000000-0000-0000-0000-000000000000.json', 'FAILED', 'SUCCESS', '2026-06-28 10:01:00'),
    (7213, 2101, 5503, NULL, NULL, 'TEST', '2026-07-24 10:01:00', '2026-07-24 10:08:00',
     '/gaze/2101/gaze-7213-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-07-24 10:01:00'),
    (7214, 2103, 5505, NULL, NULL, 'TEST', '2026-07-10 11:01:00', '2026-07-10 11:10:00',
     '/gaze/2103/gaze-7214-00000000-0000-0000-0000-000000000000.json', 'FAILED', 'FAILED', '2026-07-10 11:01:00'),
    (7215, 2104, 5506, NULL, NULL, 'TEST', '2026-07-18 12:01:00', '2026-07-18 12:08:00',
     '/gaze/2104/gaze-7215-00000000-0000-0000-0000-000000000000.json', 'COMPLETED', 'SUCCESS', '2026-07-18 12:01:00');

INSERT INTO `gaze_analysis_results`
    (`id`, `gaze_session_id`, `total_visited_duration`, `total_visited_count`,
     `reverse_read_count`, `avg_visited_duration`, `created_at`)
VALUES
    (7301, 7201, 42400, 68, 7, 624, '2026-07-05 09:08:00'),
    (7302, 7202, 35800, 59, 5, 607, '2026-07-24 09:08:00'),
    (7303, 7204, 29500, 48, 3, 615, '2026-07-18 11:07:00'),
    (7311, 7211, 51200, 82, 9, 624, '2026-05-30 10:11:00'),
    (7312, 7213, 40800, 67, 5, 609, '2026-07-24 10:09:00'),
    (7313, 7215, 33600, 52, 2, 646, '2026-07-18 12:09:00');

INSERT INTO `word_attempt_logs`
    (`id`, `student_id`, `word_id`, `training_id`, `use_location`, `surface_text`,
     `has_audio_data`, `fixation_duration_ms`, `fixation_count`,
     `gaze_start_offset_ms`, `gaze_end_offset_ms`, `is_skipped`, `regression_count`,
     `pronunciation_accuracy_score`, `speech_start_offset_ms`, `speech_end_offset_ms`,
     `is_correct`, `created_at`, `total_score`, `question_no`, `target_index`, `token_index`, `is_final`)
VALUES
    (8201, 2101, 1, 4201, 'TRAINING', '사과', TRUE, 900, 2, 0, 900, FALSE, 0, 720, 0, 1100, TRUE, '2026-07-05 09:03:00', 720, 1, 0, 0, TRUE),
    (8202, 2101, 2, 4201, 'TRAINING', '바나나', TRUE, 1100, 2, 1000, 2100, FALSE, 1, 680, 1200, 2500, FALSE, '2026-07-05 09:04:00', 680, 1, 1, 1, TRUE),
    (8203, 2101, 3, 4201, 'TRAINING', '학교', TRUE, 1000, 2, 2200, 3200, FALSE, 0, 750, 2700, 3900, TRUE, '2026-07-05 09:05:00', 750, 1, 2, 2, TRUE),
    (8204, 2101, 1, 4203, 'TRAINING', '사과', TRUE, 700, 1, 0, 700, FALSE, 0, 880, 0, 850, TRUE, '2026-07-24 09:03:00', 880, 1, 0, 0, TRUE),
    (8205, 2101, 2, 4203, 'TRAINING', '바나나', TRUE, 800, 1, 800, 1600, FALSE, 0, 850, 900, 1750, TRUE, '2026-07-24 09:04:00', 850, 1, 1, 1, TRUE),
    (8206, 2101, 3, 4203, 'TRAINING', '학교', TRUE, 750, 1, 1700, 2450, FALSE, 0, 900, 1850, 2650, TRUE, '2026-07-24 09:05:00', 900, 1, 2, 2, TRUE),
    (8210, 2103, 1, 4210, 'TRAINING', '사과', TRUE, 1200, 3, 0, 1200, FALSE, 1, 760, 0, 1400, TRUE, '2026-07-27 10:03:00', 760, 1, 0, 0, TRUE),
    (8211, 2103, 2, 4210, 'TRAINING', '바나나', TRUE, 1400, 3, 1300, 2700, FALSE, 1, 700, 1500, 3100, FALSE, '2026-07-27 10:04:00', 700, 1, 1, 1, TRUE),
    (8212, 2103, 3, 4210, 'TRAINING', '학교', TRUE, 1300, 2, 2800, 4100, FALSE, 1, 780, 3200, 4700, TRUE, '2026-07-27 10:05:00', 780, 1, 2, 2, TRUE);

INSERT INTO `reports`
    (`id`, `student_id`, `start_date`, `end_date`, `snapshot_data`, `teacher_memo`, `created_at`)
VALUES
    (9101, 2101, '2026-07-01 00:00:00', '2026-07-27 23:59:59',
     '{"learningDays":2,"totalTrainingTimeMinutes":29,"completedTrainingCount":4,"averageAccuracy":78.5,"averageReadingSpeed":70.5,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-07-05","accuracy":69,"readingSpeed":60,"pronunciationScore":72},{"date":"2026-07-24","accuracy":88,"readingSpeed":81,"pronunciationScore":87}],"areaAchievements":[{"area":"낱말 읽기","achievement":78},{"area":"문장 읽기","achievement":79}],"frequentlyIncorrectWords":[],"improvedPatterns":["낱말 읽기 정확도 향상"],"persistentDifficultyPatterns":["받침이 포함된 문장 읽기"],"gazeTrend":{"generatedAt":"2026-07-27T15:10:00","training":{"status":"AVAILABLE","comparisonAvailable":true,"points":[],"changes":null,"descriptions":[],"failedSessionCount":0},"test":{"status":"AVAILABLE","comparisonAvailable":true,"points":[],"changes":null,"descriptions":[],"failedSessionCount":1}}}',
     '최근 낱말 읽기 정확도와 읽기 속도가 함께 향상되었습니다.', '2026-07-27 15:10:00'),
    (9102, 2101, '2026-06-01 00:00:00', '2026-06-30 23:59:59',
     '{"learningDays":1,"totalTrainingTimeMinutes":15,"completedTrainingCount":2,"averageAccuracy":69,"averageReadingSpeed":60,"readingSpeedUnit":"CPM","growthHistory":[],"areaAchievements":[],"frequentlyIncorrectWords":[],"improvedPatterns":[],"persistentDifficultyPatterns":[],"gazeTrend":{"generatedAt":"2026-07-01T09:20:00","training":{"status":"NO_DATA","comparisonAvailable":false,"points":[],"changes":null,"descriptions":[],"failedSessionCount":0},"test":{"status":"FAILED","comparisonAvailable":false,"points":[],"changes":null,"descriptions":[],"failedSessionCount":1}}}',
     NULL, '2026-07-01 09:20:00'),
    (9103, 2103, '2026-07-01 00:00:00', '2026-07-27 23:59:59',
     '{"learningDays":2,"totalTrainingTimeMinutes":34,"completedTrainingCount":4,"averageAccuracy":68.25,"averageReadingSpeed":54,"readingSpeedUnit":"CPM","growthHistory":[],"areaAchievements":[{"area":"문장 이해","achievement":68}],"frequentlyIncorrectWords":[],"improvedPatterns":[],"persistentDifficultyPatterns":["문장 핵심 내용 찾기"],"gazeTrend":{"generatedAt":"2026-07-27T17:40:00","training":{"status":"FAILED","comparisonAvailable":false,"points":[],"changes":null,"descriptions":[],"failedSessionCount":1},"test":{"status":"FAILED","comparisonAvailable":false,"points":[],"changes":null,"descriptions":[],"failedSessionCount":1}}}',
     '문장 이해 훈련을 다음 커리큘럼에도 포함해 주세요.', '2026-07-27 17:40:00');

INSERT INTO `stories`
    (`id`, `student_id`, `story_template_id`, `created_at`, `status`, `progress`)
VALUES
    (6401, 2101, 1, '2026-07-20 16:00:00', 'COMPLETED', 100),
    (6402, 2103, 2, '2026-07-15 16:00:00', 'COMPLETED', 100),
    (6403, 2104, 1, '2026-07-18 16:00:00', 'IN_PROGRESS', 50),
    (6404, 2107, 2, '2026-07-24 16:00:00', 'COMPLETED', 100);

INSERT INTO `story_scenes`
    (`scene_id`, `story_id`, `image_url`, `sequence_no`, `created_at`)
VALUES
    (6501, 6401, NULL, 1, '2026-07-20 16:00:00'),
    (6502, 6402, NULL, 1, '2026-07-15 16:00:00'),
    (6503, 6403, NULL, 1, '2026-07-18 16:00:00'),
    (6504, 6404, NULL, 1, '2026-07-24 16:00:00');

INSERT INTO `story_lines`
    (`id`, `scene_id`, `has_choices`, `content`, `sequence_no`, `created_at`, `read_at`)
VALUES
    (6601, 6501, FALSE, JSON_OBJECT('text', '하늘이는 별빛 숲에서 작은 친구를 만났어요.'), 1, '2026-07-20 16:00:00', '2026-07-20 16:02:00'),
    (6602, 6502, FALSE, JSON_OBJECT('text', '서아는 구름 우체국에 편지를 전해 주었어요.'), 1, '2026-07-15 16:00:00', '2026-07-15 16:03:00'),
    (6603, 6503, TRUE, JSON_OBJECT('text', '우진이는 두 갈래 길 앞에서 잠시 생각했어요.'), 1, '2026-07-18 16:00:00', NULL),
    (6604, 6504, FALSE, JSON_OBJECT('text', '지민이는 반짝이는 구름 기차에 올라탔어요.'), 1, '2026-07-24 16:00:00', '2026-07-24 16:02:00');

INSERT INTO `characters`
    (`id`, `student_id`, `story_id`, `image_url`, `created_at`, `name`)
VALUES
    (6701, 2101, 6401, NULL, '2026-07-20 16:05:00', '별빛 토끼'),
    (6702, 2103, 6402, NULL, '2026-07-15 16:05:00', '구름 새'),
    (6703, 2104, 6403, NULL, '2026-07-18 16:05:00', '숲길 여우'),
    (6704, 2107, 6404, NULL, '2026-07-24 16:05:00', '구름 기관사');

-- Normalize demo student gender values.
UPDATE students
SET gender = 'Girl'
WHERE id = 2001
  AND gender = 'girl';


-- Complete demo training questions.
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


-- Add the second demo learner.
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
    (6202, 6102, FALSE, JSON_OBJECT('text', '한결이는 구름 우체국에서 파란 편지의 주인을 찾았어요.'), 1,
     '2026-07-22 10:00:00', '2026-07-22 10:01:00');

INSERT INTO `characters`
    (`id`, `student_id`, `story_id`, `image_url`, `created_at`, `name`)
VALUES
    (6302, 2002, 6002, NULL, '2026-07-22 10:05:00', '구름 우체부');


-- Expand demo daily curricula.
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

-- Normalize teacher persona learning questions.
-- Fresh persona fixtures already use the accepted learner question JSON contract,
-- so the historical in-place JSON conversion is intentionally omitted from V2.
-- Limit the learner demo to three students.
-- Keep the learner app focused on three students without deleting the richer
-- teacher-dashboard history for the remaining demo personas.
INSERT INTO `teachers`
    (`id`, `email`, `password`, `name`, `organization`, `created_at`, `gender`, `image_url`)
SELECT
    1999,
    'archive@iread.local',
    source_teacher.`password`,
    '데모 보관 교사',
    '아이리드 데모 보관함',
    '2026-07-30 09:00:00',
    NULL,
    NULL
FROM `teachers` source_teacher
WHERE source_teacher.`id` = 1001
  AND NOT EXISTS (
      SELECT 1
      FROM `teachers` existing_teacher
      WHERE existing_teacher.`id` = 1999
         OR existing_teacher.`email` = 'archive@iread.local'
  );

UPDATE `students`
SET `teacher_id` = 1999
WHERE `teacher_id` = 1001
  AND `id` NOT IN (2001, 2002, 2103);

-- Close older active curricula so each selected student has one unambiguous
-- current curriculum.
UPDATE `daily_curriculums`
SET `status` = 'COMPLETED',
    `completed_at` = COALESCE(`completed_at`, '2026-07-30 08:59:59')
WHERE `student_id` IN (2001, 2002, 2103)
  AND `status` IN ('NOT_STARTED', 'IN_PROGRESS');

INSERT INTO `daily_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (180001, 2001, 'NOT_STARTED', '2026-07-30 09:00:00', NULL),
    (180002, 2002, 'NOT_STARTED', '2026-07-30 09:01:00', NULL),
    (180003, 2103, 'NOT_STARTED', '2026-07-30 09:02:00', NULL);

-- All five templates below have an empty requiredInputs policy, so the
-- learner can complete these curricula without a microphone.
INSERT INTO `trainings`
    (`id`, `training_template_id`, `daily_curriculum_id`, `sequence_no`,
     `created_at`, `started_at`, `finished_at`, `status`, `result`, `accuracy`)
SELECT
    181000 + students.slot_no * 10 + templates.sequence_no,
    templates.training_template_id,
    180000 + students.slot_no,
    templates.sequence_no,
    TIMESTAMPADD(
        MINUTE,
        students.slot_no * 10 + templates.sequence_no,
        '2026-07-30 09:00:00'
    ),
    NULL,
    NULL,
    CASE WHEN templates.sequence_no = 1 THEN 'NOT_STARTED' ELSE 'NOT_READY' END,
    NULL,
    NULL
FROM (
    SELECT 1 AS slot_no
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) students
CROSS JOIN (
    SELECT 1 AS sequence_no, 4 AS training_template_id
    UNION ALL SELECT 2, 5
    UNION ALL SELECT 3, 7
    UNION ALL SELECT 4, 8
    UNION ALL SELECT 5, 11
) templates;

INSERT INTO `training_datas`
    (`id`, `train_id`, `generated_data`, `created_at`)
SELECT
    182000 + students.slot_no * 10 + questions.sequence_no,
    181000 + students.slot_no * 10 + questions.sequence_no,
    questions.generated_data,
    TIMESTAMPADD(
        MINUTE,
        students.slot_no * 10 + questions.sequence_no,
        '2026-07-30 09:00:00'
    )
FROM (
    SELECT 1 AS slot_no
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) students
CROSS JOIN (
    SELECT
        1 AS sequence_no,
        JSON_OBJECT(
            'schemaVersion', 2,
            'questions', JSON_ARRAY(
                JSON_OBJECT(
                    'questionId', 'no-mic-consonant-sound',
                    'questionNo', 1,
                    'type', 'CONSONANT_SOUND_CHOICE',
                    'requiredInputs', JSON_ARRAY(),
                    'content', JSON_OBJECT(
                        'audioText', 'ㄱ',
                        'choices', JSON_ARRAY('ㄱ', 'ㄴ', 'ㄷ')
                    ),
                    'answer', JSON_OBJECT('answerIndex', 0)
                )
            )
        ) AS generated_data
    UNION ALL
    SELECT
        2,
        JSON_OBJECT(
            'schemaVersion', 2,
            'questions', JSON_ARRAY(
                JSON_OBJECT(
                    'questionId', 'no-mic-vowel-sound',
                    'questionNo', 1,
                    'type', 'VOWEL_SOUND_CHOICE',
                    'requiredInputs', JSON_ARRAY(),
                    'content', JSON_OBJECT(
                        'audioText', 'ㅏ',
                        'choices', JSON_ARRAY('ㅏ', 'ㅓ', 'ㅗ')
                    ),
                    'answer', JSON_OBJECT('answerIndex', 0)
                )
            )
        )
    UNION ALL
    SELECT
        3,
        JSON_OBJECT(
            'schemaVersion', 2,
            'questions', JSON_ARRAY(
                JSON_OBJECT(
                    'questionId', 'no-mic-syllable-initial',
                    'questionNo', 1,
                    'type', 'SYLLABLE_INITIAL_CHOICE',
                    'requiredInputs', JSON_ARRAY(),
                    'content', JSON_OBJECT(
                        'audioText', '가',
                        'choices', JSON_ARRAY('ㄱ', 'ㄴ', 'ㅁ')
                    ),
                    'answer', JSON_OBJECT('answerIndex', 0)
                )
            )
        )
    UNION ALL
    SELECT
        4,
        JSON_OBJECT(
            'schemaVersion', 2,
            'questions', JSON_ARRAY(
                JSON_OBJECT(
                    'questionId', 'no-mic-word-initial',
                    'questionNo', 1,
                    'type', 'WORD_INITIAL_CHOICE',
                    'requiredInputs', JSON_ARRAY(),
                    'content', JSON_OBJECT(
                        'audioText', '나무',
                        'choices', JSON_ARRAY('ㄱ', 'ㄴ', 'ㄷ')
                    ),
                    'answer', JSON_OBJECT('answerIndex', 1)
                )
            )
        )
    UNION ALL
    SELECT
        5,
        JSON_OBJECT(
            'schemaVersion', 2,
            'questions', JSON_ARRAY(
                JSON_OBJECT(
                    'questionId', 'no-mic-word-final',
                    'questionNo', 1,
                    'type', 'WORD_FINAL_SOUND_CHOICE',
                    'requiredInputs', JSON_ARRAY(),
                    'content', JSON_OBJECT(
                        'targetAudioText', '산',
                        'choices', JSON_ARRAY('ㄱ', 'ㄴ', 'ㅁ')
                    ),
                    'answer', JSON_OBJECT('answerIndex', 1)
                )
            )
        )
) questions;

-- Add Saetbyeol's all-training showcase curriculum.
-- Replace Saetbyeol's active five-item curriculum with a showcase curriculum.
-- Training rows and their single generated questions are populated after the
-- 34 canonical templates have been initialized by DemoAllTrainingCurriculumInitializer.
UPDATE `daily_curriculums`
SET `status` = 'COMPLETED',
    `completed_at` = COALESCE(`completed_at`, '2026-07-30 11:15:00')
WHERE `student_id` = 2001
  AND `status` IN ('NOT_STARTED', 'IN_PROGRESS');

INSERT INTO `daily_curriculums`
    (`id`, `student_id`, `status`, `created_at`, `completed_at`)
VALUES
    (190001, 2001, 'NOT_STARTED', '2026-07-30 11:16:00', NULL);

-- 시선 지표가 있는 단어 시도만 시선 사용으로 표시한다.
UPDATE `word_attempt_logs`
SET `has_gaze_data` = (`fixation_duration_ms` IS NOT NULL OR `fixation_count` IS NOT NULL);
