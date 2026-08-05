-- Canonical QA demo dataset. This script is idempotent and is shared by
-- Flyway's one-time installation and the explicit qaDemoReset command.

DELETE FROM auth_refresh_sessions WHERE teacher_id = 1001;
DELETE FROM password_reset_tokens WHERE teacher_id = 1001;

UPDATE teachers
SET email = 'test@test.com',
    password = '$2a$10$vfiy7KBnt1J1WNY1e/AMpuGU2Jbf95qaYXAkZ50CC0HK06Zuu1TIi',
    name = '시연교수자',
    organization = 'ssafy'
WHERE id = 1001;

UPDATE students
SET name = '김OO',
    birthday = '2018-04-12',
    gender = 'Boy',
    school = '시연초',
    guardian = '김보호',
    guardian_contact = '010-0000-3001',
    guardian_email = 'demo@demo1.com',
    address = '가상시 데모구 읽기길 101',
    image_url = '/images/student-profile-boy.png',
    teacher_memo = '[관찰] 받침 끝소리와 받침 뒤 모음 연음을 어려워합니다. [흥미] 자동차와 F1 레이싱에 집중도가 높습니다.'
WHERE id = 2001;

UPDATE students
SET name = '이OO',
    birthday = '2017-02-14',
    gender = 'Girl',
    school = '샛별초',
    guardian = '이사랑',
    guardian_contact = '010-0000-3002',
    guardian_email = 'demo@demo2.com',
    address = '가상시 샛별구 배움로 202',
    image_url = '/images/student-profile-girl.png',
    teacher_memo = '[관찰] 초성과 받침 위치의 ㄴ을 ㄷ 또는 ㅁ과 선택적으로 혼동합니다. 낱말 속 단서에서는 반응이 좋아집니다.'
WHERE id = 2002;

UPDATE students
SET name = '박OO',
    birthday = '2016-01-27',
    gender = 'Boy',
    school = '샛별초',
    guardian = '박다정',
    guardian_contact = '010-0000-3003',
    guardian_email = 'demo@demo3.com',
    address = '가상시 샛별구 이야기길 303',
    image_url = '/images/student-profile-boy.png',
    teacher_memo = '[관찰] 읽기 정확도와 이해도는 높지만 속도가 느립니다. 반복 읽기로 자연스러운 유창성을 지원합니다.'
WHERE id = 2103;

-- Replace every story belonging to the three visible demo students.
DELETE FROM gaze_analysis_results
WHERE gaze_session_id IN (
    SELECT id FROM gaze_sessions
    WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))
);

DELETE FROM gaze_sessions
WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103));

DELETE FROM word_attempt_logs
WHERE story_line_id IN (
    SELECT id FROM story_lines
    WHERE scene_id IN (
        SELECT scene_id FROM story_scenes
        WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))
    )
);

DELETE FROM story_page_edit_audits
WHERE story_line_id IN (
    SELECT id FROM story_lines
    WHERE scene_id IN (
        SELECT scene_id FROM story_scenes
        WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))
    )
);

DELETE FROM story_choices
WHERE story_line_id IN (
    SELECT id FROM story_lines
    WHERE scene_id IN (
        SELECT scene_id FROM story_scenes
        WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))
    )
);

DELETE FROM story_lines
WHERE scene_id IN (
    SELECT scene_id FROM story_scenes
    WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))
);

DELETE FROM story_scenes
WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103));

DELETE FROM characters
WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103));

DELETE FROM stories WHERE student_id IN (2001, 2002, 2103);

INSERT INTO stories (id, student_id, story_template_id, created_at, status, progress)
VALUES
    (280001, 2001, 2, '2026-07-14 15:00:00', 'COMPLETED', 100),
    (280002, 2001, 1, '2026-08-04 15:00:00', 'IN_PROGRESS', 60),
    (280003, 2002, 4, '2026-07-16 15:00:00', 'COMPLETED', 100),
    (280004, 2002, 5, '2026-08-03 15:00:00', 'IN_PROGRESS', 50),
    (280005, 2103, 6, '2026-07-18 15:00:00', 'COMPLETED', 100),
    (280006, 2103, 3, '2026-08-02 15:00:00', 'IN_PROGRESS', 60);

INSERT INTO story_scenes (scene_id, story_id, image_url, sequence_no, created_at)
VALUES
    (281011, 280001, '/uploads/images/5cce6a09-9535-4e1f-8507-52652f2deca9.jpg', 1, '2026-07-14 15:00:00'),
    (281012, 280001, '/uploads/images/1b6e8aba-1076-43fb-a9f7-40b4ba68cac6.jpg', 2, '2026-07-14 15:05:00'),
    (281021, 280002, '/uploads/images/37e5becf-afeb-4472-9b4b-f4ad31804ad7.jpg', 1, '2026-08-04 15:00:00'),
    (281022, 280002, '/uploads/images/fac73006-704f-40b1-abf5-ce4b298d6e33.jpg', 2, '2026-08-04 15:05:00'),
    (281031, 280003, '/uploads/images/77b0b1b1-2794-40d4-903f-54b00f2b03fd.jpg', 1, '2026-07-16 15:00:00'),
    (281032, 280003, '/uploads/images/2f4abe13-8f84-4d87-b711-06cfe13674c5.jpg', 2, '2026-07-16 15:05:00'),
    (281041, 280004, '/uploads/images/098f386f-8b72-4940-b9b3-d4d197e42dbc.jpg', 1, '2026-08-03 15:00:00'),
    (281042, 280004, '/uploads/images/dcfbbd01-bc15-4691-bddb-dd9314826709.jpg', 2, '2026-08-03 15:05:00'),
    (281051, 280005, '/uploads/images/badf86e5-24c2-4401-920e-51e8f2ce00ac.jpg', 1, '2026-07-18 15:00:00'),
    (281052, 280005, '/uploads/images/5863d881-12c4-44b1-ae36-7cafe2d60108.jpg', 2, '2026-07-18 15:05:00'),
    (281061, 280006, '/uploads/images/347242ee-73de-4179-bebc-95f4f41d3bdc.jpg', 1, '2026-08-02 15:00:00'),
    (281062, 280006, '/uploads/images/8d07dd90-efa6-4885-9d54-92f40bd7fa9f.jpg', 2, '2026-08-02 15:05:00');

INSERT INTO story_lines
    (id, scene_id, has_choices, content, branch_prompt, sequence_no, created_at, read_at, revision)
VALUES
    (282001, 281011, FALSE, JSON_OBJECT('text', '햇살이 따뜻한 여름날, 개미들은 겨울에 먹을 곡식과 열매를 부지런히 모았어요.'), NULL, 1, '2026-07-14 15:01:00', '2026-07-14 15:02:00', 0),
    (282002, 281012, TRUE, JSON_OBJECT('text', '겨울이 오자 배짱이는 준비의 소중함을 깨달았고, 개미들은 따뜻한 음식을 함께 나누었어요.'), JSON_OBJECT('subtitle', '배짱이는 다음 여름을 어떻게 보낼까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '개미들과 함께 먹이를 준비해요.'), JSON_OBJECT('optionNo', 2, 'label', '노래와 준비 시간을 나누어 계획해요.'), JSON_OBJECT('optionNo', 3, 'label', '친구들에게 배운 점을 이야기해요.'))), 1, '2026-07-14 15:06:00', '2026-07-14 15:07:00', 0),
    (282003, 281021, FALSE, JSON_OBJECT('text', '김OO는 토끼와 거북이가 레이싱복을 입고 출발선에 선 모습을 보았어요. 두 선수는 안전하게 마지막 랩을 시작했어요.'), NULL, 1, '2026-08-04 15:01:00', '2026-08-04 15:02:00', 0),
    (282004, 281022, TRUE, JSON_OBJECT('text', '거북이는 코너에서 바퀴를 단단히 붙이고 천천히 방향을 잡았어요. 마지막 직선에서 토끼와 나란히 결승선을 향했어요.'), JSON_OBJECT('subtitle', '마지막 직선에서 어떤 작전을 선택할까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '아껴 둔 터보를 사용해요.'), JSON_OBJECT('optionNo', 2, 'label', '토끼와 나란히 달려요.'), JSON_OBJECT('optionNo', 3, 'label', '피트 크루에게 작전을 물어요.'))), 1, '2026-08-04 15:06:00', NULL, 0),
    (282005, 281031, FALSE, JSON_OBJECT('text', '요정 할머니의 따뜻한 마법으로 신데렐라의 옷은 반짝이는 드레스로 바뀌고 호박은 멋진 마차가 되었어요.'), NULL, 1, '2026-07-16 15:01:00', '2026-07-16 15:02:00', 0),
    (282006, 281032, TRUE, JSON_OBJECT('text', '신데렐라는 무도회에서 즐겁게 춤을 추었지만, 자정이 가까워지자 약속을 기억하고 서둘러 돌아갔어요.'), JSON_OBJECT('subtitle', '신데렐라는 유리구두를 어떻게 할까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '소중히 간직해요.'), JSON_OBJECT('optionNo', 2, 'label', '친절한 친구에게 맡겨요.'), JSON_OBJECT('optionNo', 3, 'label', '궁전으로 돌아가 찾아요.'))), 1, '2026-07-16 15:06:00', '2026-07-16 15:07:00', 0),
    (282007, 281041, FALSE, JSON_OBJECT('text', '별주부는 바닷가에서 이OO에게 토끼와 함께 용궁으로 가는 길을 보여 주었어요.'), NULL, 1, '2026-08-03 15:01:00', '2026-08-03 15:02:00', 0),
    (282008, 281042, TRUE, JSON_OBJECT('text', '용궁에 도착한 토끼는 침착하게 생각한 뒤 지혜로운 말로 모두가 놀랄 해결책을 들려주었어요.'), JSON_OBJECT('subtitle', '토끼는 다음에 무엇을 할까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '별주부와 솔직하게 이야기해요.'), JSON_OBJECT('optionNo', 2, 'label', '용왕에게 다른 약을 찾아보자고 해요.'), JSON_OBJECT('optionNo', 3, 'label', '친구들과 함께 지혜를 모아요.'))), 1, '2026-08-03 15:06:00', NULL, 0),
    (282009, 281051, FALSE, JSON_OBJECT('text', '아기돼지 삼형제는 짚과 나무와 벽돌을 준비해 각자의 집을 정성껏 지었어요.'), NULL, 1, '2026-07-18 15:01:00', '2026-07-18 15:02:00', 0),
    (282010, 281052, TRUE, JSON_OBJECT('text', '세 형제는 튼튼한 벽돌집에 함께 모여 서로 도우며 안전하게 지낼 수 있었어요.'), JSON_OBJECT('subtitle', '세 형제는 집을 더 튼튼하게 만들기 위해 무엇을 할까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '함께 벽돌을 더 쌓아요.'), JSON_OBJECT('optionNo', 2, 'label', '창문과 문을 점검해요.'), JSON_OBJECT('optionNo', 3, 'label', '이웃과 안전 방법을 나눠요.'))), 1, '2026-07-18 15:06:00', '2026-07-18 15:07:00', 0),
    (282011, 281061, FALSE, JSON_OBJECT('text', '박OO는 잔잔한 아침 바다에서 노인이 작은 배를 저으며 커다란 물고기와 인사하는 모습을 천천히 읽었어요.'), NULL, 1, '2026-08-02 15:01:00', '2026-08-02 15:03:00', 0),
    (282012, 281062, TRUE, JSON_OBJECT('text', '노인은 포기하지 않고 노을빛 항구로 돌아왔고, 푸른 물고기도 배 곁에서 힘차게 헤엄쳤어요.'), JSON_OBJECT('subtitle', '노인은 항구에 도착해 무엇을 할까요?', 'options', JSON_ARRAY(JSON_OBJECT('optionNo', 1, 'label', '바다 친구에게 고마움을 전해요.'), JSON_OBJECT('optionNo', 2, 'label', '오늘의 모험을 천천히 기록해요.'), JSON_OBJECT('optionNo', 3, 'label', '마을 사람들과 바다를 돌봐요.'))), 1, '2026-08-02 15:06:00', NULL, 0);

INSERT INTO story_choices (id, story_line_id, content, created_at)
VALUES
    (283001, 282002, '노래와 준비 시간을 나누어 계획해요.', '2026-07-14 15:07:00'),
    (283002, 282004, '토끼와 나란히 달려요.', '2026-08-04 15:07:00'),
    (283003, 282006, '소중히 간직해요.', '2026-07-16 15:07:00'),
    (283004, 282008, '친구들과 함께 지혜를 모아요.', '2026-08-03 15:07:00'),
    (283005, 282010, '함께 벽돌을 더 쌓아요.', '2026-07-18 15:07:00'),
    (283006, 282012, '오늘의 모험을 천천히 기록해요.', '2026-08-02 15:07:00');

INSERT INTO characters (id, student_id, story_id, image_url, created_at, name)
VALUES
    (284001, 2001, 280002, '/uploads/images/37e5becf-afeb-4472-9b4b-f4ad31804ad7.jpg', '2026-08-04 15:00:30', 'F1 거북이'),
    (284002, 2002, 280004, '/uploads/images/098f386f-8b72-4940-b9b3-d4d197e42dbc.jpg', '2026-08-03 15:00:30', '별주부'),
    (284003, 2103, 280006, '/uploads/images/347242ee-73de-4179-bebc-95f4f41d3bdc.jpg', '2026-08-02 15:00:30', '바다 노인');

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data_url, status, calibration_status, created_at)
VALUES
    (290101, 2001, NULL, NULL, 280002, 'STORY', '2026-08-04 15:00:00', '2026-08-04 15:09:00', '/gaze/2001/gaze-290101-a0010000-0000-4000-8000-000000000001.json', 'COMPLETED', 'SUCCESS', '2026-08-04 15:00:00'),
    (290102, 2002, NULL, NULL, 280004, 'STORY', '2026-08-03 15:00:00', '2026-08-03 15:08:00', '/gaze/2002/gaze-290102-a0020000-0000-4000-8000-000000000002.json', 'COMPLETED', 'SUCCESS', '2026-08-03 15:00:00'),
    (290103, 2103, NULL, NULL, 280006, 'STORY', '2026-08-02 15:00:00', '2026-08-02 15:12:00', '/gaze/2103/gaze-290103-a0030000-0000-4000-8000-000000000003.json', 'COMPLETED', 'SUCCESS', '2026-08-02 15:00:00');

INSERT INTO gaze_analysis_results
    (id, gaze_session_id, total_visited_duration, total_visited_count, reverse_read_count,
     avg_visited_duration, sentence_metrics, regressions, analysis_meta, created_at)
VALUES
    (291001, 290101, 27800, 32, 5, 869,
     JSON_ARRAY(
         JSON_OBJECT('storyLineId', 282003, 'sequenceNo', 1, 'surfaceText', '김OO는 토끼와 거북이가 레이싱복을 입고 출발선에 선 모습을 보았어요.', 'dwellDurationMs', 16800, 'fixationCount', 19, 'regressionCount', 4, 'firstGazeOffsetMs', 400, 'lastGazeOffsetMs', 17200),
         JSON_OBJECT('storyLineId', 282004, 'sequenceNo', 2, 'surfaceText', '거북이는 마지막 직선에서 토끼와 나란히 결승선을 향했어요.', 'dwellDurationMs', 11000, 'fixationCount', 13, 'regressionCount', 1, 'firstGazeOffsetMs', 18000, 'lastGazeOffsetMs', 29000)
     ),
     JSON_ARRAY(JSON_OBJECT('pageNo', 1, 'sequenceNo', 1, 'fromOffsetMs', 6100, 'toOffsetMs', 4800, 'durationMs', 1300)),
     JSON_OBJECT('source', 'qa-demo', 'persona', '받침·연음', 'calculationVersion', 'story-gaze-word-v1'), '2026-08-04 15:09:10'),
    (291002, 290102, 16000, 22, 3, 727,
     JSON_ARRAY(
         JSON_OBJECT('storyLineId', 282007, 'sequenceNo', 1, 'surfaceText', '별주부는 이OO에게 용궁으로 가는 길을 보여 주었어요.', 'dwellDurationMs', 9800, 'fixationCount', 14, 'regressionCount', 2, 'firstGazeOffsetMs', 300, 'lastGazeOffsetMs', 10100),
         JSON_OBJECT('storyLineId', 282008, 'sequenceNo', 2, 'surfaceText', '토끼는 지혜로운 말로 모두가 놀랄 해결책을 들려주었어요.', 'dwellDurationMs', 6200, 'fixationCount', 8, 'regressionCount', 1, 'firstGazeOffsetMs', 10800, 'lastGazeOffsetMs', 17000)
     ),
     JSON_ARRAY(JSON_OBJECT('pageNo', 1, 'sequenceNo', 1, 'fromOffsetMs', 4200, 'toOffsetMs', 3500, 'durationMs', 700)),
     JSON_OBJECT('source', 'qa-demo', 'persona', 'ㄴ 선택적 혼동', 'calculationVersion', 'story-gaze-word-v1'), '2026-08-03 15:08:10'),
    (291003, 290103, 27800, 24, 1, 1158,
     JSON_ARRAY(
         JSON_OBJECT('storyLineId', 282011, 'sequenceNo', 1, 'surfaceText', '박OO는 노인이 작은 배를 저으며 물고기와 인사하는 모습을 천천히 읽었어요.', 'dwellDurationMs', 14500, 'fixationCount', 12, 'regressionCount', 1, 'firstGazeOffsetMs', 500, 'lastGazeOffsetMs', 15000),
         JSON_OBJECT('storyLineId', 282012, 'sequenceNo', 2, 'surfaceText', '노인은 포기하지 않고 노을빛 항구로 돌아왔어요.', 'dwellDurationMs', 13300, 'fixationCount', 12, 'regressionCount', 0, 'firstGazeOffsetMs', 15800, 'lastGazeOffsetMs', 29100)
     ),
     JSON_ARRAY(),
     JSON_OBJECT('source', 'qa-demo', 'persona', '높은 정확도·느린 속도', 'calculationVersion', 'story-gaze-word-v1'), '2026-08-02 15:12:10');

-- These rows are also the one-time post-seed marker. They are inserted after
-- ReadingFeatureDataInitializer has installed the feature catalog.
DELETE FROM student_feature_profiles WHERE id BETWEEN 299001 AND 299006;

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299001, 2001, id, 0.5000, 510, 47.00, 840, 5.20, 2.30, 0.12, 16800, 860, 0.9400, 20,
       '2026-08-04 15:09:00', '2026-08-04 15:09:10'
FROM reading_features WHERE feature_code = 'PHONOLOGY.LIAISON.CODA_TO_SILENT_ONSET';

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299002, 2001, id, 0.6100, 590, 34.00, 760, 4.70, 1.90, 0.09, 14100, 790, 0.9100, 24,
       '2026-08-04 15:09:00', '2026-08-04 15:09:10'
FROM reading_features WHERE feature_code = 'SYLLABLE.CVC';

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299003, 2002, id, 0.5400, 570, 39.00, 680, 4.10, 1.60, 0.07, 12300, 820, 0.9200, 18,
       '2026-08-03 15:08:00', '2026-08-03 15:08:10'
FROM reading_features WHERE feature_code = 'GRAPHEME.ONSET.BASIC.ㄴ';

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299004, 2002, id, 0.5800, 600, 35.00, 710, 4.30, 1.80, 0.08, 12800, 790, 0.9000, 16,
       '2026-08-03 15:08:00', '2026-08-03 15:08:10'
FROM reading_features WHERE feature_code = 'GRAPHEME.CODA.SIMPLE.ㄴ';

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299005, 2103, id, 0.9100, 910, 6.00, 1160, 3.20, 0.40, 0.02, 27800, 280, 0.9500, 30,
       '2026-08-02 15:12:00', '2026-08-02 15:12:10'
FROM reading_features WHERE feature_code = 'SENTENCE.FLUENCY';

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 299006, 2103, id, 0.9400, 930, 4.00, 980, 3.00, 0.20, 0.01, 24500, 190, 0.9600, 32,
       '2026-08-02 15:12:00', '2026-08-02 15:12:10'
FROM reading_features WHERE feature_code = 'SENTENCE.SIMPLE';
