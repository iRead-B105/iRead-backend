-- Reproducible showcase data for the teacher web.
-- This script runs after the training-template and reading-feature initializers.

UPDATE students
SET gender = 'Girl',
    teacher_memo = '낱말 읽기는 안정적이며, 받침과 긴 문장 읽기를 집중 관찰하고 있습니다.'
WHERE id = 2001;

UPDATE students
SET teacher_memo = '기초 음운부터 문장 유창성까지 전체 훈련 유형을 체험한 데모 학습자입니다. 받침과 의미 단위 끊어 읽기를 다음 목표로 권장합니다.'
WHERE id = 2103;

INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
VALUES
    (3301, 2103, 'COMPLETED', '2026-01-12 10:00:00', '2026-01-12 10:35:00'),
    (3302, 2103, 'COMPLETED', '2026-02-09 10:00:00', '2026-02-09 10:38:00'),
    (3303, 2103, 'COMPLETED', '2026-03-16 10:00:00', '2026-03-16 10:42:00'),
    (3304, 2103, 'COMPLETED', '2026-04-13 10:00:00', '2026-04-13 10:40:00'),
    (3305, 2103, 'COMPLETED', '2026-05-18 10:00:00', '2026-05-18 10:44:00'),
    (3306, 2103, 'COMPLETED', '2026-06-22 10:00:00', '2026-06-22 10:46:00'),
    (3307, 2103, 'COMPLETED', '2026-07-28 10:00:00', '2026-07-28 10:39:00'),
    (3308, 2103, 'NOT_STARTED', '2026-07-29 09:00:00', NULL);

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    43000 + tt.id,
    tt.id,
    3300 + CEIL(tt.id / 5),
    MOD(tt.id - 1, 5) + 1,
    TIMESTAMP(DATE_ADD('2026-01-12', INTERVAL (CEIL(tt.id / 5) - 1) MONTH), '10:00:00'),
    TIMESTAMP(DATE_ADD('2026-01-12', INTERVAL (CEIL(tt.id / 5) - 1) MONTH),
              MAKETIME(10, MOD(tt.id - 1, 5) * 7 + 1, 0)),
    TIMESTAMP(DATE_ADD('2026-01-12', INTERVAL (CEIL(tt.id / 5) - 1) MONTH),
              MAKETIME(10, MOD(tt.id - 1, 5) * 7 + 6, 0)),
    'COMPLETED',
    JSON_OBJECT(
        'learningAssessment',
        CASE
            WHEN MOD(tt.id, 4) = 0 THEN CONCAT(tt.name, '에서 어려움이 관찰되어 반복 연습을 권장합니다.')
            WHEN MOD(tt.id, 3) = 0 THEN CONCAT(tt.name, '의 정확도와 속도가 이전보다 향상되었습니다.')
            ELSE CONCAT(tt.name, '을 안정적으로 수행했습니다.')
        END,
        'retryCount', MOD(tt.id, 4),
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNumber', 1, 'question', CONCAT(tt.name, ' 첫 번째 문항'),
                        'isCorrect', TRUE, 'correctAnswer', '정답 보기', 'selectedAnswer', '정답 보기'),
            JSON_OBJECT('questionNumber', 2, 'question', CONCAT(tt.name, ' 두 번째 문항'),
                        'isCorrect', MOD(tt.id, 3) <> 0, 'correctAnswer', '바른 응답',
                        'selectedAnswer', IF(MOD(tt.id, 3) <> 0, '바른 응답', '유사한 응답')),
            JSON_OBJECT('questionNumber', 3, 'question', CONCAT(tt.name, ' 세 번째 문항'),
                        'isCorrect', MOD(tt.id, 4) <> 0, 'correctAnswer', '또박또박 읽기',
                        'selectedAnswer', IF(MOD(tt.id, 4) <> 0, '또박또박 읽기', '일부 생략'))
        )
    ),
    560 + MOD(tt.id * 47, 390)
FROM training_templates tt
WHERE tt.id BETWEEN 1 AND 34;

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
VALUES
    (43101, 22, 3308, 1, '2026-07-29 09:00:00', NULL, NULL, 'NOT_STARTED', NULL, NULL),
    (43102, 25, 3308, 2, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL),
    (43103, 28, 3308, 3, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL),
    (43104, 32, 3308, 4, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL),
    (43105, 34, 3308, 5, '2026-07-29 09:00:00', NULL, NULL, 'NOT_READY', NULL, NULL);

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    44000 + tt.id,
    43000 + tt.id,
    JSON_OBJECT(
        'version', 2,
        'trainingType', JSON_UNQUOTE(JSON_EXTRACT(tt.prompt, '$.trainingType')),
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionId', CONCAT('showcase-', tt.id, '-1'), 'questionNo', 1,
                        'problem', JSON_OBJECT('targetText', CONCAT(tt.name, ': 소리와 글자를 확인해 보세요.')),
                        'answer', JSON_OBJECT('correctText', '소리와 글자를 정확하게 연결합니다.')),
            JSON_OBJECT('questionId', CONCAT('showcase-', tt.id, '-2'), 'questionNo', 2,
                        'problem', JSON_OBJECT('targetText', CONCAT(tt.name, ': 알맞은 답을 골라 보세요.')),
                        'answer', JSON_OBJECT('correctText', '문맥에 맞는 답을 선택합니다.')),
            JSON_OBJECT('questionId', CONCAT('showcase-', tt.id, '-3'), 'questionNo', 3,
                        'problem', JSON_OBJECT('targetText', CONCAT(tt.name, ': 또박또박 읽어 보세요.')),
                        'answer', JSON_OBJECT('correctText', '생략 없이 자연스럽게 읽습니다.'))
        )
    ),
    '2026-07-29 09:05:00'
FROM training_templates tt
WHERE tt.id BETWEEN 1 AND 34;

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    44100 + current_training.sequence_no,
    current_training.id,
    JSON_OBJECT(
        'version', 2,
        'trainingType', JSON_UNQUOTE(JSON_EXTRACT(tt.prompt, '$.trainingType')),
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionId', CONCAT('current-', current_training.sequence_no, '-1'),
                        'questionNo', 1,
                        'problem', JSON_OBJECT('targetText', CONCAT(tt.name, ': 오늘의 첫 문항입니다.')),
                        'answer', JSON_OBJECT('correctText', '차분하게 읽고 응답합니다.')),
            JSON_OBJECT('questionId', CONCAT('current-', current_training.sequence_no, '-2'),
                        'questionNo', 2,
                        'problem', JSON_OBJECT('targetText', CONCAT(tt.name, ': 오늘의 두 번째 문항입니다.')),
                        'answer', JSON_OBJECT('correctText', '의미를 생각하며 읽습니다.'))
        )
    ),
    '2026-07-29 09:05:00'
FROM trainings current_training
JOIN training_templates tt ON tt.id = current_training.training_template_id
WHERE current_training.id BETWEEN 43101 AND 43105;

INSERT INTO words (id, content, length)
VALUES
    (10001, '토끼', 2), (10002, '산책', 2), (10003, '꽃밭', 2),
    (10004, '읽습니다', 4), (10005, '국물', 2), (10006, '신라', 2),
    (10007, '같이', 2), (10008, '놀이터', 3), (10009, '친구', 2),
    (10010, '도서관', 3);

INSERT INTO word_categories (id, word_id, category_name)
VALUES
    (10101, 10001, '받침없는단어'), (10102, 10002, '받침단어'),
    (10103, 10003, '겹받침단어'), (10104, 10004, '문장읽기'),
    (10105, 10005, '비음화'), (10106, 10006, '유음화'),
    (10107, 10007, '구개음화'), (10108, 10008, '받침없는단어'),
    (10109, 10009, '받침없는단어'), (10110, 10010, '다음절단어');

INSERT INTO word_attempt_logs
    (id, student_id, word_id, training_id, use_location, surface_text, has_audio_data,
     fixation_duration_ms, fixation_count, gaze_start_offset_ms, gaze_end_offset_ms,
     is_skipped, regression_count, pronunciation_accuracy_score, speech_start_offset_ms,
     speech_end_offset_ms, is_correct, created_at, total_score, question_no,
     target_index, token_index, is_final)
VALUES
    (8301, 2103, 10001, 43022, 'TRAINING', '토끼', TRUE, 720, 1, 0, 720, FALSE, 0, 910, 0, 820, TRUE, '2026-05-18 10:03:00', 910, 1, 0, 0, TRUE),
    (8302, 2103, 10002, 43022, 'TRAINING', '산책', TRUE, 1380, 3, 800, 2180, FALSE, 2, 620, 900, 2450, FALSE, '2026-05-18 10:04:00', 610, 1, 1, 1, FALSE),
    (8303, 2103, 10002, 43022, 'TRAINING', '산책', TRUE, 1050, 2, 800, 1850, FALSE, 1, 780, 900, 2100, TRUE, '2026-05-18 10:05:00', 760, 1, 1, 1, TRUE),
    (8304, 2103, 10003, 43022, 'TRAINING', '꽃밭', TRUE, 1550, 4, 1900, 3450, FALSE, 2, 590, 2200, 3900, FALSE, '2026-05-18 10:06:00', 570, 2, 0, 0, TRUE),
    (8305, 2103, 10004, 43025, 'TRAINING', '읽습니다', TRUE, 1180, 2, 0, 1180, FALSE, 1, 740, 0, 1450, TRUE, '2026-05-18 10:10:00', 730, 1, 0, 0, TRUE),
    (8306, 2103, 10005, 43034, 'TRAINING', '국물', TRUE, 1420, 3, 0, 1420, FALSE, 2, 610, 0, 1650, FALSE, '2026-07-12 10:03:00', 600, 1, 0, 0, TRUE),
    (8307, 2103, 10006, 43034, 'TRAINING', '신라', TRUE, 1320, 3, 1500, 2820, FALSE, 1, 670, 1700, 3150, FALSE, '2026-07-12 10:04:00', 650, 1, 1, 1, TRUE),
    (8308, 2103, 10007, 43034, 'TRAINING', '같이', TRUE, 880, 1, 2900, 3780, FALSE, 0, 860, 3250, 4200, TRUE, '2026-07-12 10:05:00', 850, 1, 2, 2, TRUE);

INSERT INTO test_curriculums (id, student_id, status, created_at, completed_at)
VALUES
    (5601, 2103, 'COMPLETED', '2026-04-20 11:00:00', '2026-04-20 11:12:00'),
    (5602, 2103, 'COMPLETED', '2026-06-29 11:00:00', '2026-06-29 11:11:00'),
    (5603, 2103, 'COMPLETED', '2026-07-29 11:00:00', '2026-07-29 11:09:00');

INSERT INTO tests
    (id, test_curriculum_id, training_template_id, status, result, accuracy,
     created_at, started_at, finished_at, sequence_no)
VALUES
    (5701, 5601, 22, 'COMPLETED',
     '{"overallScore":61,"readingTimeSeconds":168,"solvingTimeSeconds":245,"gazeDepartureCount":8,"questions":[{"questionNumber":1,"question":"꽃밭을 읽어 보세요.","isCorrect":false,"correctAnswer":"꽃밭","selectedAnswer":"꼳밭"},{"questionNumber":2,"question":"토끼가 산책합니다를 읽어 보세요.","isCorrect":true,"correctAnswer":"토끼가 산책합니다.","selectedAnswer":"토끼가 산책합니다."},{"questionNumber":3,"question":"국물을 읽어 보세요.","isCorrect":false,"correctAnswer":"궁물","selectedAnswer":"국물"}]}',
     61, '2026-04-20 11:00:00', '2026-04-20 11:01:00', '2026-04-20 11:12:00', 1),
    (5702, 5602, 25, 'COMPLETED',
     '{"overallScore":74,"readingTimeSeconds":132,"solvingTimeSeconds":205,"gazeDepartureCount":5,"questions":[{"questionNumber":1,"question":"친구와 학교에 갑니다를 읽어 보세요.","isCorrect":true,"correctAnswer":"친구와 학교에 갑니다.","selectedAnswer":"친구와 학교에 갑니다."},{"questionNumber":2,"question":"토끼가 꽃밭을 산책합니다를 읽어 보세요.","isCorrect":false,"correctAnswer":"토끼가 꽃밭을 산책합니다.","selectedAnswer":"토끼가 꽃을 산책합니다."},{"questionNumber":3,"question":"도서관에서 책을 읽습니다를 읽어 보세요.","isCorrect":true,"correctAnswer":"도서관에서 책을 읽습니다.","selectedAnswer":"도서관에서 책을 읽습니다."}]}',
     74, '2026-06-29 11:00:00', '2026-06-29 11:01:00', '2026-06-29 11:11:00', 1),
    (5703, 5603, 34, 'COMPLETED',
     '{"overallScore":86,"readingTimeSeconds":104,"solvingTimeSeconds":171,"gazeDepartureCount":2,"questions":[{"questionNumber":1,"question":"짧은 이야기의 첫 문장을 읽어 보세요.","isCorrect":true,"correctAnswer":"토끼는 친구와 숲길을 걸었습니다.","selectedAnswer":"토끼는 친구와 숲길을 걸었습니다."},{"questionNumber":2,"question":"이야기의 중심 내용을 말해 보세요.","isCorrect":true,"correctAnswer":"친구와 함께 길을 찾았습니다.","selectedAnswer":"친구와 함께 길을 찾았습니다."},{"questionNumber":3,"question":"마지막 문장을 읽어 보세요.","isCorrect":true,"correctAnswer":"모두 함께 집으로 돌아왔습니다.","selectedAnswer":"모두 함께 집으로 돌아왔습니다."}]}',
     86, '2026-07-29 11:00:00', '2026-07-29 11:01:00', '2026-07-29 11:09:00', 1);

INSERT INTO test_datas (id, test_id, generated_data, created_at)
VALUES
    (5801, 5701, '{"version":2,"questions":[{"questionNo":1,"problem":{"targetText":"꽃밭"},"answer":{"correctText":"꽃밭"}},{"questionNo":2,"problem":{"targetText":"토끼가 산책합니다."},"answer":{"correctText":"토끼가 산책합니다."}},{"questionNo":3,"problem":{"targetText":"국물"},"answer":{"correctText":"궁물"}}]}', '2026-04-20 11:00:00'),
    (5802, 5702, '{"version":2,"questions":[{"questionNo":1,"problem":{"targetText":"친구와 학교에 갑니다."},"answer":{"correctText":"친구와 학교에 갑니다."}},{"questionNo":2,"problem":{"targetText":"토끼가 꽃밭을 산책합니다."},"answer":{"correctText":"토끼가 꽃밭을 산책합니다."}},{"questionNo":3,"problem":{"targetText":"도서관에서 책을 읽습니다."},"answer":{"correctText":"도서관에서 책을 읽습니다."}}]}', '2026-06-29 11:00:00'),
    (5803, 5703, '{"version":2,"questions":[{"questionNo":1,"problem":{"targetText":"토끼는 친구와 숲길을 걸었습니다."},"answer":{"correctText":"토끼는 친구와 숲길을 걸었습니다."}},{"questionNo":2,"problem":{"targetText":"이야기의 중심 내용은 무엇인가요?"},"answer":{"correctText":"친구와 함께 길을 찾았습니다."}},{"questionNo":3,"problem":{"targetText":"모두 함께 집으로 돌아왔습니다."},"answer":{"correctText":"모두 함께 집으로 돌아왔습니다."}}]}', '2026-07-29 11:00:00');

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
VALUES
    (7401, 2103, NULL, 43022, NULL, 'TRAINING', '2026-05-18 10:01:00', '2026-05-18 10:06:00',
     '[{"timestampMs":0,"x":0.31,"y":0.42},{"timestampMs":200,"x":0.36,"y":0.43},{"timestampMs":400,"x":0.44,"y":0.44}]',
     'COMPLETED', 'SUCCESS', '2026-05-18 10:01:00'),
    (7402, 2103, NULL, 43025, NULL, 'TRAINING', '2026-05-18 10:08:00', '2026-05-18 10:13:00',
     '[]', 'FAILED', 'SUCCESS', '2026-05-18 10:08:00'),
    (7403, 2103, 5701, NULL, NULL, 'TEST', '2026-04-20 11:01:00', '2026-04-20 11:12:00',
     '[{"timestampMs":0,"x":0.24,"y":0.38},{"timestampMs":200,"x":0.55,"y":0.41}]',
     'COMPLETED', 'SUCCESS', '2026-04-20 11:01:00'),
    (7404, 2103, 5702, NULL, NULL, 'TEST', '2026-06-29 11:01:00', '2026-06-29 11:11:00',
     '[]', 'FAILED', 'FAILED', '2026-06-29 11:01:00'),
    (7405, 2103, 5703, NULL, NULL, 'TEST', '2026-07-29 11:01:00', '2026-07-29 11:09:00',
     '[{"timestampMs":0,"x":0.28,"y":0.40},{"timestampMs":200,"x":0.40,"y":0.41},{"timestampMs":400,"x":0.52,"y":0.42}]',
     'COMPLETED', 'SUCCESS', '2026-07-29 11:01:00');

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT 9601, 2103, id, 0.91, 905, 0.08, 580, 1.40, 0.30, 0.02, 920, 180, 0.94, 18, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'SYLLABLE.CV'
UNION ALL
SELECT 9602, 2103, id, 0.67, 650, 0.31, 980, 2.80, 1.40, 0.12, 1480, 760, 0.88, 15, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'SYLLABLE.COMPLEX_CODA'
UNION ALL
SELECT 9603, 2103, id, 0.63, 620, 0.35, 1040, 3.10, 1.60, 0.10, 1550, 810, 0.91, 17, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'PHONOLOGY.NASALIZATION'
UNION ALL
SELECT 9604, 2103, id, 0.78, 770, 0.19, 810, 2.10, 0.80, 0.05, 1210, 520, 0.86, 14, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'WORD.AUTOMATICITY'
UNION ALL
SELECT 9605, 2103, id, 0.72, 735, 0.23, 920, 2.50, 1.10, 0.07, 1390, 640, 0.90, 19, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'SENTENCE.PHRASE_BOUNDARY'
UNION ALL
SELECT 9606, 2103, id, 0.84, 830, 0.14, 690, 1.70, 0.50, 0.03, 1080, 350, 0.93, 22, '2026-07-29 11:10:00', '2026-07-29 11:12:00'
FROM reading_features WHERE feature_code = 'SENTENCE.FLUENCY';

INSERT INTO reports
    (id, student_id, start_date, end_date, snapshot_data, teacher_memo, created_at)
VALUES
    (9201, 2103, '2026-04-01 00:00:00', '2026-04-30 23:59:59',
     '{"learningDays":2,"totalTrainingTimeMinutes":52,"completedTrainingCount":7,"averageAccuracy":63.4,"averageReadingSpeed":48,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-04-13","accuracy":62,"readingSpeed":46,"pronunciationScore":64},{"date":"2026-04-20","accuracy":61,"readingSpeed":48,"pronunciationScore":66}],"areaAchievements":[{"area":"음절 읽기","achievement":72},{"area":"받침 읽기","achievement":56},{"area":"문장 읽기","achievement":61}],"frequentlyIncorrectWords":[{"word":"꽃밭","count":3},{"word":"국물","count":2}],"improvedPatterns":["기본 음절 읽기"],"persistentDifficultyPatterns":["겹받침 읽기","비음화 적용"],"gazeTrend":{"generatedAt":"2026-04-30T17:00:00","training":{"status":"AVAILABLE","comparisonAvailable":false,"points":[],"changes":null,"descriptions":["낱말 끝부분에서 시선 고정 시간이 깁니다."],"failedSessionCount":0},"test":{"status":"AVAILABLE","comparisonAvailable":false,"points":[],"changes":null,"descriptions":["되읽기 횟수가 관찰됩니다."],"failedSessionCount":0}}}',
     '받침 낱말을 짧게 나누어 읽는 연습이 필요합니다.', '2026-04-30 17:00:00'),
    (9202, 2103, '2026-06-01 00:00:00', '2026-06-30 23:59:59',
     '{"learningDays":4,"totalTrainingTimeMinutes":88,"completedTrainingCount":12,"averageAccuracy":74.2,"averageReadingSpeed":61,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-06-12","accuracy":69,"readingSpeed":54,"pronunciationScore":70},{"date":"2026-06-22","accuracy":76,"readingSpeed":60,"pronunciationScore":75},{"date":"2026-06-29","accuracy":74,"readingSpeed":61,"pronunciationScore":77}],"areaAchievements":[{"area":"낱말 읽기","achievement":78},{"area":"문장 읽기","achievement":74},{"area":"읽기 유창성","achievement":70}],"frequentlyIncorrectWords":[{"word":"꽃밭","count":2},{"word":"읽습니다","count":2}],"improvedPatterns":["낱말 읽기 자동성","문장 읽기 속도"],"persistentDifficultyPatterns":["의미 단위 끊어 읽기"],"gazeTrend":{"generatedAt":"2026-06-30T17:00:00","training":{"status":"AVAILABLE","comparisonAvailable":true,"points":[],"changes":{"reverseReadCount":-2},"descriptions":["시선 되돌아가기가 감소했습니다."],"failedSessionCount":1},"test":{"status":"FAILED","comparisonAvailable":true,"points":[],"changes":null,"descriptions":["최근 검사 한 건은 보정 실패로 분석하지 못했습니다."],"failedSessionCount":1}}}',
     '읽기 속도는 향상되었으며 문장 호흡을 계속 지도해 주세요.', '2026-06-30 17:00:00'),
    (9203, 2103, '2026-07-28 00:00:00', '2026-07-29 23:59:59',
     '{"learningDays":2,"totalTrainingTimeMinutes":48,"completedTrainingCount":4,"averageAccuracy":86,"averageReadingSpeed":72,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-07-28","accuracy":82,"readingSpeed":69,"pronunciationScore":81},{"date":"2026-07-29","accuracy":86,"readingSpeed":72,"pronunciationScore":85}],"areaAchievements":[{"area":"짧은 글 읽기","achievement":84},{"area":"문장 유창성","achievement":86},{"area":"내용 이해","achievement":88}],"frequentlyIncorrectWords":[{"word":"국물","count":1}],"improvedPatterns":["문장 유창성","내용 이해"],"persistentDifficultyPatterns":["비음화 낱말"],"gazeTrend":{"generatedAt":"2026-07-29T17:00:00","training":{"status":"AVAILABLE","comparisonAvailable":true,"points":[],"changes":{"avgVisitedDuration":-42,"reverseReadCount":-3},"descriptions":["평균 시선 고정 시간과 되읽기가 감소했습니다."],"failedSessionCount":0},"test":{"status":"AVAILABLE","comparisonAvailable":true,"points":[],"changes":{"avgVisitedDuration":-23,"reverseReadCount":-2},"descriptions":["검사 중 시선 흐름이 이전보다 안정적입니다."],"failedSessionCount":0}}}',
     '전체 읽기 흐름이 안정되었습니다. 다음에는 비음화 낱말을 복습합니다.', '2026-07-29 17:00:00');

INSERT INTO stories (id, student_id, story_template_id, created_at, status, progress)
VALUES (6801, 2103, 1, '2026-07-29 15:00:00', 'IN_PROGRESS', 60);

INSERT INTO story_scenes (scene_id, story_id, image_url, sequence_no, created_at)
VALUES
    (6811, 6801, NULL, 1, '2026-07-29 15:00:00'),
    (6812, 6801, NULL, 2, '2026-07-29 15:05:00');

INSERT INTO story_lines (id, scene_id, has_choices, content, sequence_no, created_at, read_at)
VALUES
    (6821, 6811, FALSE, '서아는 숲속 도서관에서 빛나는 지도를 발견했어요.', 1, '2026-07-29 15:00:00', '2026-07-29 15:02:00'),
    (6822, 6811, TRUE, '지도에는 두 갈래 길이 그려져 있었어요. 어느 길로 가 볼까요?', 2, '2026-07-29 15:02:00', '2026-07-29 15:04:00'),
    (6823, 6812, TRUE, '길 끝에서 작은 문을 만났어요. 다음에는 무엇을 할까요?', 1, '2026-07-29 15:05:00', NULL);

INSERT INTO story_choices (id, story_line_id, content, created_at)
VALUES
    (6831, 6822, '별빛이 비치는 왼쪽 길로 간다.', '2026-07-29 15:03:00'),
    (6834, 6823, '문을 조심스럽게 두드린다.', '2026-07-29 15:05:00');

INSERT INTO `character` (id, student_id, story_id, image_url, created_at, name)
VALUES (6841, 2103, 6801, NULL, '2026-07-29 15:01:00', '지도 요정 루미');

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
VALUES
    (7406, 2103, NULL, NULL, 6801, 'STORY', '2026-07-29 15:00:00', NULL,
     '[{"timestampMs":0,"x":0.30,"y":0.39},{"timestampMs":200,"x":0.42,"y":0.40}]',
     'RUNNING', 'SUCCESS', '2026-07-29 15:00:00');

-- Keep the hand-authored report fixtures aligned with ReportSnapshot.
UPDATE reports
SET snapshot_data = '{"learningDays":2,"totalTrainingTimeMinutes":52,"completedTrainingCount":7,"averageAccuracy":63.4,"averageReadingSpeed":48,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-04-13","accuracy":62,"readingSpeed":46,"pronunciationScore":64},{"date":"2026-04-20","accuracy":61,"readingSpeed":48,"pronunciationScore":66}],"areaAchievements":[{"area":"음절 읽기","achievement":72},{"area":"받침 읽기","achievement":56},{"area":"문장 읽기","achievement":61}],"frequentlyIncorrectWords":[{"wordId":10003,"wordName":"꽃밭","attemptCount":3,"incorrectCount":2,"incorrectRate":66.67},{"wordId":10005,"wordName":"국물","attemptCount":2,"incorrectCount":1,"incorrectRate":50}],"improvedPatterns":["기본 음절 읽기"],"persistentDifficultyPatterns":["겹받침 읽기","비음화 적용"],"gazeAnalysis":{"gazeAnalysisResultId":7502,"totalDwellTime":62400,"dwellCount":96,"regressionCount":12,"averageFixationTime":650},"gazeTrend":{"generatedAt":"2026-04-30T17:00:00","training":{"status":"NO_DATA","comparisonAvailable":false,"points":[],"changes":null,"descriptions":["해당 기간의 훈련 시선 기록이 없습니다."],"failedSessionCount":0},"test":{"status":"AVAILABLE","comparisonAvailable":false,"points":[{"gazeAnalysisResultId":7502,"gazeSessionId":7403,"sourceType":"TEST","sourceId":5701,"analyzedAt":"2026-04-20T11:13:00","totalVisitedDurationMs":62400,"totalVisitedCount":96,"reverseReadCount":12,"avgVisitedDurationMs":650}],"changes":null,"descriptions":["첫 검사 시선 기준 기록입니다."],"failedSessionCount":0}}}'
WHERE id = 9201;

UPDATE reports
SET snapshot_data = '{"learningDays":4,"totalTrainingTimeMinutes":88,"completedTrainingCount":12,"averageAccuracy":74.2,"averageReadingSpeed":61,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-06-12","accuracy":69,"readingSpeed":54,"pronunciationScore":70},{"date":"2026-06-22","accuracy":76,"readingSpeed":60,"pronunciationScore":75},{"date":"2026-06-29","accuracy":74,"readingSpeed":61,"pronunciationScore":77}],"areaAchievements":[{"area":"음절 읽기","achievement":78},{"area":"문장 읽기","achievement":74},{"area":"읽기 유창성","achievement":70}],"frequentlyIncorrectWords":[{"wordId":10003,"wordName":"꽃밭","attemptCount":4,"incorrectCount":2,"incorrectRate":50},{"wordId":10004,"wordName":"읽습니다","attemptCount":3,"incorrectCount":2,"incorrectRate":66.67}],"improvedPatterns":["음절 읽기 자동화","문장 읽기 속도"],"persistentDifficultyPatterns":["의미 단위 끊어 읽기"],"gazeAnalysis":{"gazeAnalysisResultId":7501,"totalDwellTime":48600,"dwellCount":76,"regressionCount":9,"averageFixationTime":639},"gazeTrend":{"generatedAt":"2026-06-30T17:00:00","training":{"status":"AVAILABLE","comparisonAvailable":false,"points":[{"gazeAnalysisResultId":7501,"gazeSessionId":7401,"sourceType":"TRAINING","sourceId":43022,"analyzedAt":"2026-05-18T10:07:00","totalVisitedDurationMs":48600,"totalVisitedCount":76,"reverseReadCount":9,"avgVisitedDurationMs":639}],"changes":null,"descriptions":["훈련 중 시선 체류 기준 기록입니다."],"failedSessionCount":1},"test":{"status":"FAILED","comparisonAvailable":false,"points":[],"changes":null,"descriptions":["최근 검사 한 건은 보정 실패로 분석하지 못했습니다."],"failedSessionCount":1}}}'
WHERE id = 9202;

UPDATE reports
SET snapshot_data = '{"learningDays":2,"totalTrainingTimeMinutes":48,"completedTrainingCount":4,"averageAccuracy":86,"averageReadingSpeed":72,"readingSpeedUnit":"CPM","growthHistory":[{"date":"2026-07-28","accuracy":82,"readingSpeed":69,"pronunciationScore":81},{"date":"2026-07-29","accuracy":86,"readingSpeed":72,"pronunciationScore":85}],"areaAchievements":[{"area":"짧은 글 읽기","achievement":84},{"area":"문장 유창성","achievement":86},{"area":"내용 이해","achievement":88}],"frequentlyIncorrectWords":[{"wordId":10005,"wordName":"국물","attemptCount":3,"incorrectCount":1,"incorrectRate":33.33}],"improvedPatterns":["문장 유창성","내용 이해"],"persistentDifficultyPatterns":["비음화 음절"],"gazeAnalysis":{"gazeAnalysisResultId":7503,"totalDwellTime":38900,"dwellCount":62,"regressionCount":3,"averageFixationTime":627},"gazeTrend":{"generatedAt":"2026-07-29T17:00:00","training":{"status":"AVAILABLE","comparisonAvailable":false,"points":[{"gazeAnalysisResultId":7501,"gazeSessionId":7401,"sourceType":"TRAINING","sourceId":43022,"analyzedAt":"2026-05-18T10:07:00","totalVisitedDurationMs":48600,"totalVisitedCount":76,"reverseReadCount":9,"avgVisitedDurationMs":639}],"changes":null,"descriptions":["훈련 시선 기준 기록을 제공합니다."],"failedSessionCount":1},"test":{"status":"AVAILABLE","comparisonAvailable":true,"points":[{"gazeAnalysisResultId":7502,"gazeSessionId":7403,"sourceType":"TEST","sourceId":5701,"analyzedAt":"2026-04-20T11:13:00","totalVisitedDurationMs":62400,"totalVisitedCount":96,"reverseReadCount":12,"avgVisitedDurationMs":650},{"gazeAnalysisResultId":7503,"gazeSessionId":7405,"sourceType":"TEST","sourceId":5703,"analyzedAt":"2026-07-29T11:10:00","totalVisitedDurationMs":38900,"totalVisitedCount":62,"reverseReadCount":3,"avgVisitedDurationMs":627}],"changes":{"totalVisitedDurationMs":{"first":62400,"latest":38900,"delta":-23500},"totalVisitedCount":{"first":96,"latest":62,"delta":-34},"reverseReadCount":{"first":12,"latest":3,"delta":-9},"avgVisitedDurationMs":{"first":650,"latest":627,"delta":-23}},"descriptions":["검사 중 시선 체류와 되돌아보기가 감소했습니다."],"failedSessionCount":1}}}'
WHERE id = 9203;
