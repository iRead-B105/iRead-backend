CREATE TEMPORARY TABLE demo_personas (
    persona_no INT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    persona_title VARCHAR(80) NOT NULL,
    teacher_memo VARCHAR(1000) NOT NULL,
    strength_area VARCHAR(80) NOT NULL,
    weakness_area VARCHAR(80) NOT NULL,
    base_accuracy INT NOT NULL,
    trend_delta INT NOT NULL,
    reading_speed INT NOT NULL,
    past_gaze_failure BOOLEAN NOT NULL,
    story_status VARCHAR(30) NOT NULL,
    story_progress INT NOT NULL
);

INSERT INTO demo_personas
    (persona_no, student_id, persona_title, teacher_memo, strength_area, weakness_area,
     base_accuracy, trend_delta, reading_speed, past_gaze_failure, story_status, story_progress)
VALUES
    (1, 2001, '글자 탐색을 시작한 초기 학습자',
     '[시연 페르소나] 글자와 소리를 연결하는 단계입니다. 그림 단서에는 적극적으로 반응하며 자음·모음 구별을 짧게 반복하면 집중력이 유지됩니다.',
     '그림 단서 활용', '자음·모음 연결', 570, 45, 34, FALSE, 'IN_PROGRESS', 35),
    (2, 2101, '꾸준히 성장하는 균형형 학습자',
     '[시연 페르소나] 음절 읽기와 문장 이해가 함께 성장하고 있습니다. 최근 정확도와 읽기 속도가 모두 안정적으로 상승해 다음 단계 확장이 적절합니다.',
     '음절 읽기', '긴 문장 호흡', 720, 35, 58, FALSE, 'COMPLETED', 100),
    (3, 2102, '기초 검사를 마친 신규 전입 학습자',
     '[시연 페르소나] 신규 전입 후 기초 검사를 완료했습니다. 낱글자 정확도는 양호하지만 학습 이력이 짧아 현재 커리큘럼에서 충분한 근거를 수집해야 합니다.',
     '낱글자 인식', '학습 근거 축적', 680, 20, 42, FALSE, 'IN_PROGRESS', 20),
    (4, 2103, '전 영역을 체험한 종합 시연 학습자',
     '[시연 페르소나] 기초 음운부터 문장 유창성까지 전체 훈련 유형을 경험했습니다. 받침과 의미 단위 끊어 읽기를 다음 목표로 권장합니다.',
     '내용 이해', '받침·끊어 읽기', 810, 28, 72, TRUE, 'IN_PROGRESS', 60),
    (5, 2104, '시선 보정 실패를 극복한 회복형 학습자',
     '[시연 페르소나] 과거 시선 보정 실패가 있었으나 재검사에서는 안정적으로 측정되었습니다. 되돌아보기 횟수가 줄고 문장 끝까지 읽는 비율이 높아졌습니다.',
     '재시도 지속성', '시선 고정 안정화', 640, 52, 49, TRUE, 'COMPLETED', 100),
    (6, 2105, '정확하지만 천천히 읽는 신중형 학습자',
     '[시연 페르소나] 정답 정확도는 높지만 낱말을 확인하는 시간이 깁니다. 시간 압박 없이 짧은 문장을 반복해 자동화 속도를 높이는 것이 목표입니다.',
     '높은 정확도', '읽기 자동화 속도', 870, 18, 38, FALSE, 'IN_PROGRESS', 50),
    (7, 2106, '빠르게 읽지만 누락이 잦은 속도 우선형 학습자',
     '[시연 페르소나] 읽기 속도는 빠르지만 조사와 받침을 건너뛰는 경향이 있습니다. 속도를 낮추고 문장 경계를 표시하면 정확도가 개선됩니다.',
     '빠른 읽기 속도', '낱말·받침 누락', 610, 30, 84, TRUE, 'IN_PROGRESS', 70),
    (8, 2107, '이야기 몰입도가 높은 서사형 학습자',
     '[시연 페르소나] 이야기 선택과 내용 예측에 적극적이며 맥락을 활용한 이해가 강점입니다. 처음 보는 낱말을 문맥에 의존하지 않고 해독하는 연습이 필요합니다.',
     '이야기 이해·예측', '새 낱말 해독', 760, 32, 66, FALSE, 'COMPLETED', 100),
    (9, 2108, '비음화 발음에 집중하는 발음 교정형 학습자',
     '[시연 페르소나] 국물·읽는 등 비음화가 포함된 낱말에서 발음 오류가 반복됩니다. 시각적 음절 분리와 느린 모범 발음을 함께 제시하고 있습니다.',
     '음절 분리', '비음화 발음', 590, 42, 46, FALSE, 'IN_PROGRESS', 45),
    (10, 2109, '검사 긴장을 완화해 가는 자신감 회복형 학습자',
     '[시연 페르소나] 훈련에서는 안정적이지만 검사 상황에서 속도와 정확도가 낮아집니다. 짧은 성공 경험을 먼저 제공한 뒤 검사 시간을 점진적으로 늘립니다.',
     '훈련 참여도', '검사 상황 긴장', 670, 48, 52, TRUE, 'COMPLETED', 100),
    (11, 2110, '문장 의미 연결이 강한 이해 중심형 학습자',
     '[시연 페르소나] 중심 내용 찾기와 그림-문장 연결이 강점입니다. 소리 내어 읽을 때 문장 부호를 반영한 억양과 호흡을 보완하고 있습니다.',
     '중심 내용 이해', '문장 억양·호흡', 800, 24, 64, FALSE, 'IN_PROGRESS', 80),
    (12, 2111, '겹받침과 되읽기가 잦은 집중 지원형 학습자',
     '[시연 페르소나] 겹받침 낱말에서 머무는 시간이 길고 되읽기가 자주 나타납니다. 낱말을 음절 단위로 나누고 최종 시도만 평가에 반영합니다.',
     '재시도 수용성', '겹받침·되읽기', 520, 55, 41, TRUE, 'IN_PROGRESS', 30);

CREATE TEMPORARY TABLE demo_numbers (seq INT PRIMARY KEY);
INSERT INTO demo_numbers (seq)
VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12);
CREATE TEMPORARY TABLE demo_story_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_scene_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_line_numbers AS
SELECT seq FROM demo_numbers;

CREATE TEMPORARY TABLE demo_features AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS feature_no
FROM reading_features;
SET @demo_feature_count = (SELECT COUNT(*) FROM demo_features);

UPDATE students student
JOIN demo_personas persona ON persona.student_id = student.id
SET student.teacher_memo = persona.teacher_memo;

INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    120000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE WHEN number.seq < 3 THEN 'COMPLETED' ELSE 'NOT_STARTED' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-05-18 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 08:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-05-25 17:00:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 15:00:00')
        ELSE NULL
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 120000 + persona.persona_no * 10 + number.seq
);

UPDATE daily_curriculums curriculum
JOIN demo_personas persona ON persona.student_id = curriculum.student_id
SET curriculum.status = 'IN_PROGRESS',
    curriculum.completed_at = NULL
WHERE curriculum.status = 'NOT_STARTED'
  AND curriculum.id <> 120000 + persona.persona_no * 10 + 3;

UPDATE daily_curriculums curriculum
JOIN demo_personas persona
  ON curriculum.id = 120000 + persona.persona_no * 10 + 2
SET curriculum.created_at = TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 08:00:00'),
    curriculum.completed_at = TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 15:00:00');

UPDATE daily_curriculums curriculum
JOIN demo_personas persona
  ON curriculum.id = 120000 + persona.persona_no * 10 + 3
SET curriculum.status = 'NOT_STARTED',
    curriculum.created_at = TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00'),
    curriculum.completed_at = NULL;

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    130000 + persona.persona_no * 100 + number.seq,
    template.id,
    120000 + persona.persona_no * 10 + CEIL(number.seq / 4),
    MOD(number.seq - 1, 4) + 1,
    CASE
        WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:00:00')
        WHEN number.seq <= 8 THEN TIMESTAMPADD(
            HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:00:00')
        ELSE TIMESTAMPADD(HOUR, number.seq, TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00'))
    END,
    CASE WHEN number.seq <= 8 THEN
        CASE
            WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:05:00')
            ELSE TIMESTAMPADD(
                HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:05:00')
        END
    END,
    CASE WHEN number.seq <= 8 THEN
        CASE
            WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:12:00')
            ELSE TIMESTAMPADD(
                HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:12:00')
        END
    END,
    CASE WHEN number.seq <= 8 THEN 'COMPLETED'
         WHEN number.seq = 9 THEN 'NOT_STARTED'
         ELSE 'NOT_READY' END,
    CASE WHEN number.seq <= 8 THEN JSON_OBJECT(
        'learningAssessment', CONCAT(persona.persona_title, '의 ', template.name, ' 수행 결과입니다.'),
        'retryCount', CASE WHEN persona.base_accuracy + number.seq * persona.trend_delta / 4 < 700 THEN 2 ELSE 0 END,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNumber', 1, 'question', CONCAT(persona.weakness_area, ' 관련 첫 번째 문항'),
                        'isCorrect', TRUE, 'selectedAnswer', '바르게 읽었습니다.', 'correctAnswer', '바르게 읽었습니다.'),
            JSON_OBJECT('questionNumber', 2, 'question', CONCAT(template.name, ' 두 번째 문항'),
                        'isCorrect', number.seq % 3 <> 0,
                        'selectedAnswer', CASE WHEN number.seq % 3 <> 0 THEN '정확한 응답' ELSE '비슷한 소리로 읽음' END,
                        'correctAnswer', '정확한 응답'),
            JSON_OBJECT('questionNumber', 3, 'question', CONCAT(persona.strength_area, ' 확인 문항'),
                        'isCorrect', TRUE, 'selectedAnswer', '끝까지 읽었습니다.', 'correctAnswer', '끝까지 읽었습니다.')
        )
    ) END,
    CASE WHEN number.seq <= 8 THEN
        LEAST(970, GREATEST(450,
            persona.base_accuracy
            + FLOOR((number.seq - 1) / 4) * persona.trend_delta
            + (MOD(number.seq * 17 + persona.persona_no * 11, 70) - 35)
        ))
    END
FROM demo_personas persona
JOIN demo_numbers number
JOIN training_templates template
  ON template.id = 1 + MOD(persona.persona_no * 3 + number.seq - 2, 34)
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 130000 + persona.persona_no * 100 + number.seq
);

UPDATE trainings training
JOIN demo_personas persona
  ON training.id BETWEEN 130000 + persona.persona_no * 100 + 5
                     AND 130000 + persona.persona_no * 100 + 8
SET training.created_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:00:00'),
    training.started_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:05:00'),
    training.finished_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:12:00');

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    135000 + persona.persona_no * 100 + number.seq,
    130000 + persona.persona_no * 100 + number.seq,
    JSON_OBJECT(
        'schemaVersion', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-1'),
                        'questionNo', 1,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('국물')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '국물')),
                        'answer', JSON_OBJECT('expectedText', '국물')),
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-2'),
                        'questionNo', 2,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '도서관에', '갑니다.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 도서관에 갑니다.')),
                        'answer', JSON_OBJECT('expectedText', '친구와 도서관에 갑니다.')),
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-3'),
                        'questionNo', 3,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '함께', '책을', '읽는', '내용입니다.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 함께 책을 읽는 내용입니다.')),
                        'answer', JSON_OBJECT('expectedText', '친구와 함께 책을 읽는 내용입니다.'))
        )
    ),
    TIMESTAMPADD(MINUTE, 1, training.created_at)
FROM demo_personas persona
JOIN demo_numbers number
JOIN trainings training ON training.id = 130000 + persona.persona_no * 100 + number.seq
JOIN training_templates template ON template.id = training.training_template_id
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 135000 + persona.persona_no * 100 + number.seq
);

INSERT INTO test_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    140000 + persona.persona_no,
    persona.student_id,
    'COMPLETED',
    TIMESTAMPADD(DAY, persona.persona_no, '2026-04-01 10:00:00'),
    TIMESTAMPADD(DAY, persona.persona_no, '2026-07-28 11:30:00')
FROM demo_personas persona
WHERE NOT EXISTS (
    SELECT 1 FROM test_curriculums existing
    WHERE existing.id = 140000 + persona.persona_no
);

INSERT INTO tests
    (id, test_curriculum_id, training_template_id, status, result, accuracy,
     created_at, started_at, finished_at, sequence_no)
SELECT
    141000 + persona.persona_no * 10 + number.seq,
    140000 + persona.persona_no,
    1 + MOD(persona.persona_no * 5 + number.seq - 1, 34),
    'COMPLETED',
    JSON_OBJECT(
        'overallScore', LEAST(98, GREATEST(45,
            ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10))),
        'readingTimeSeconds', GREATEST(55, 190 - persona.reading_speed - number.seq * 8),
        'solvingTimeSeconds', GREATEST(90, 260 - persona.reading_speed - number.seq * 10),
        'gazeDepartureCount', GREATEST(0, 5 - number.seq),
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNumber', 1, 'question', '제시된 낱말을 소리 내어 읽어 보세요.',
                        'isCorrect', TRUE, 'selectedAnswer', '도서관', 'correctAnswer', '도서관'),
            JSON_OBJECT('questionNumber', 2, 'question', CONCAT(persona.weakness_area, '을 확인하는 문장을 읽어 보세요.'),
                        'isCorrect', number.seq > 1, 'selectedAnswer',
                        CASE WHEN number.seq > 1 THEN '친구와 함께 길을 찾았습니다.' ELSE '친구와 길을 잃었습니다.' END,
                        'correctAnswer', '친구와 함께 길을 찾았습니다.'),
            JSON_OBJECT('questionNumber', 3, 'question', '이야기의 중심 내용을 말해 보세요.',
                        'isCorrect', TRUE, 'selectedAnswer', '서로 도와 문제를 해결했습니다.',
                        'correctAnswer', '서로 도와 문제를 해결했습니다.')
        )
    ),
    LEAST(98, GREATEST(45,
        ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10))),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:00:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:00:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:05:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:13:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:13:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:13:00')
    END,
    number.seq
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM tests existing
    WHERE existing.id = 141000 + persona.persona_no * 10 + number.seq
);

INSERT INTO test_datas (id, test_id, generated_data, created_at)
SELECT
    145000 + persona.persona_no * 10 + number.seq,
    141000 + persona.persona_no * 10 + number.seq,
    JSON_OBJECT(
        'schemaVersion', 2,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNo', 1,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('도서관에서', '책을', '읽었습니다.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '도서관에서 책을 읽었습니다.')),
                        'answer', JSON_OBJECT('expectedText', '도서관에서 책을 읽었습니다.')),
            JSON_OBJECT('questionNo', 2,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '함께', '길을', '찾았습니다.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 함께 길을 찾았습니다.')),
                        'answer', JSON_OBJECT('expectedText', '친구와 함께 길을 찾았습니다.')),
            JSON_OBJECT('questionNo', 3,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('서로', '도와', '문제를', '해결했습니다.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '서로 도와 문제를 해결했습니다.')),
                        'answer', JSON_OBJECT('expectedText', '서로 도와 문제를 해결했습니다.'))
        )
    ),
    test.created_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN tests test ON test.id = 141000 + persona.persona_no * 10 + number.seq
WHERE NOT EXISTS (
    SELECT 1 FROM test_datas existing
    WHERE existing.id = 145000 + persona.persona_no * 10 + number.seq
);

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    150000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE WHEN number.seq <= 2 THEN
        141000 + persona.persona_no * 10 + CASE number.seq WHEN 1 THEN 1 ELSE 3 END
    END,
    CASE WHEN number.seq = 3 THEN 130000 + persona.persona_no * 100 + 8 END,
    NULL,
    CASE WHEN number.seq <= 2 THEN 'TEST' ELSE 'TRAINING' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:13:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:13:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:12:00')
    END,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.24 + persona.persona_no * 0.01, 'y', 0.38),
        JSON_OBJECT('timestampMs', 200, 'x', 0.40, 'y', 0.41),
        JSON_OBJECT('timestampMs', 400, 'x', 0.54, 'y', 0.43)
    ),
    CASE WHEN number.seq = 3 AND persona.past_gaze_failure THEN 'FAILED' ELSE 'COMPLETED' END,
    CASE WHEN number.seq = 3 AND persona.past_gaze_failure THEN 'FAILED' ELSE 'SUCCESS' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM gaze_sessions existing
    WHERE existing.id = 150000 + persona.persona_no * 10 + number.seq
);

INSERT INTO gaze_analysis_results
    (id, gaze_session_id, total_visited_duration, total_visited_count,
     reverse_read_count, avg_visited_duration, created_at)
SELECT
    155000 + persona.persona_no * 10 + number.seq,
    150000 + persona.persona_no * 10 + number.seq,
    GREATEST(22000, 68000 - persona.reading_speed * 260 - number.seq * persona.trend_delta * 80),
    GREATEST(38, 104 - persona.reading_speed / 2 - number.seq * 6),
    GREATEST(1, 15 - number.seq * 3 - persona.trend_delta / 20),
    GREATEST(380, 820 - persona.trend_delta * number.seq),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:14:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:14:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:13:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN gaze_sessions session ON session.id = 150000 + persona.persona_no * 10 + number.seq
WHERE session.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM gaze_analysis_results existing
      WHERE existing.id = 155000 + persona.persona_no * 10 + number.seq
  );

INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT
    160000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    feature.id,
    LEAST(0.9800, GREATEST(0.4000,
        (persona.base_accuracy + (number.seq - 2) * persona.trend_delta) / 1000)),
    LEAST(980, GREATEST(420,
        persona.base_accuracy + number.seq * 18 - CASE WHEN number.seq = 1 THEN 90 ELSE 0 END)),
    GREATEST(0.02, (1000 - persona.base_accuracy + number.seq * 12) / 20),
    GREATEST(380, 980 - persona.reading_speed * 4 + number.seq * 45),
    1.10 + number.seq * 0.35,
    0.30 + number.seq * 0.28,
    LEAST(0.35, GREATEST(0.01, (1000 - persona.base_accuracy) / 1800 + number.seq * 0.01)),
    GREATEST(650, 2100 - persona.reading_speed * 11 + number.seq * 80),
    LEAST(950, GREATEST(120,
        1000 - persona.base_accuracy + CASE WHEN number.seq = 1 THEN 180 ELSE number.seq * 25 END)),
    0.7600 + number.seq * 0.045,
    8 + persona.persona_no + number.seq * 3,
    TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 16:00:00'),
    TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 17:00:00')
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 4
JOIN demo_features feature
  ON feature.feature_no = 1 + MOD(persona.persona_no * 7 + number.seq * 5 - 1,
      @demo_feature_count)
WHERE NOT EXISTS (
    SELECT 1 FROM student_feature_profiles existing
    WHERE existing.id = 160000 + persona.persona_no * 10 + number.seq
);

INSERT INTO reports
    (id, student_id, start_date, end_date, snapshot_data, teacher_memo, created_at)
SELECT
    170000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE number.seq WHEN 1 THEN '2026-04-01 00:00:00' ELSE '2026-07-01 00:00:00' END,
    CASE number.seq WHEN 1 THEN '2026-05-31 23:59:59' ELSE '2026-07-29 23:59:59' END,
    JSON_OBJECT(
        'learningDays', CASE number.seq WHEN 1 THEN 5 ELSE 8 END,
        'totalTrainingTimeMinutes', CASE number.seq WHEN 1 THEN 58 ELSE 92 END,
        'completedTrainingCount', CASE number.seq WHEN 1 THEN 4 ELSE 8 END,
        'averageAccuracy', ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10, 1),
        'averageReadingSpeed', persona.reading_speed + (number.seq - 1) * 6,
        'readingSpeedUnit', 'CPM',
        'growthHistory', JSON_ARRAY(
            JSON_OBJECT('date', CASE number.seq WHEN 1 THEN '2026-04-12' ELSE '2026-07-10' END,
                        'accuracy', ROUND((persona.base_accuracy - 25) / 10, 1),
                        'readingSpeed', persona.reading_speed - 4,
                        'pronunciationScore', ROUND((persona.base_accuracy - 10) / 10, 1)),
            JSON_OBJECT('date', CASE number.seq WHEN 1 THEN '2026-05-20' ELSE '2026-07-28' END,
                        'accuracy', ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10, 1),
                        'readingSpeed', persona.reading_speed + (number.seq - 1) * 6,
                        'pronunciationScore', ROUND((persona.base_accuracy + persona.trend_delta) / 10, 1))
        ),
        'areaAchievements', JSON_ARRAY(
            JSON_OBJECT('area', persona.strength_area,
                        'achievement', LEAST(98, ROUND((persona.base_accuracy + 90) / 10, 1))),
            JSON_OBJECT('area', persona.weakness_area,
                        'achievement', GREATEST(40, ROUND((persona.base_accuracy - 80 + number.seq * persona.trend_delta) / 10, 1))),
            JSON_OBJECT('area', '문장 유창성',
                        'achievement', ROUND((persona.base_accuracy + persona.trend_delta / 2) / 10, 1))
        ),
        'frequentlyIncorrectWords', JSON_ARRAY(
            JSON_OBJECT('wordId', 10003, 'wordName', '꽃밭',
                        'attemptCount', 4, 'incorrectCount', CASE WHEN persona.base_accuracy < 700 THEN 3 ELSE 1 END,
                        'incorrectRate', CASE WHEN persona.base_accuracy < 700 THEN 75.00 ELSE 25.00 END),
            JSON_OBJECT('wordId', 10005, 'wordName', '국물',
                        'attemptCount', 3, 'incorrectCount', CASE WHEN persona.weakness_area LIKE '%비음화%' THEN 2 ELSE 1 END,
                        'incorrectRate', CASE WHEN persona.weakness_area LIKE '%비음화%' THEN 66.67 ELSE 33.33 END)
        ),
        'improvedPatterns', JSON_ARRAY(persona.strength_area, CONCAT(persona.weakness_area, ' 재시도')),
        'persistentDifficultyPatterns', JSON_ARRAY(persona.weakness_area),
        'gazeAnalysis', JSON_OBJECT(
            'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 2,
            'totalDwellTime', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
            'dwellCount', GREATEST(38, 104 - persona.reading_speed / 2 - 12),
            'regressionCount', GREATEST(1, 9 - persona.trend_delta / 20),
            'averageFixationTime', GREATEST(380, 820 - persona.trend_delta * 2)
        ),
        'gazeTrend', JSON_OBJECT(
            'generatedAt', CASE number.seq WHEN 1 THEN '2026-05-31T18:00:00' ELSE '2026-07-29T18:00:00' END,
            'training', JSON_OBJECT(
                'status', CASE WHEN persona.past_gaze_failure THEN 'FAILED' ELSE 'AVAILABLE' END,
                'comparisonAvailable', FALSE,
                'points', JSON_ARRAY(),
                'changes', NULL,
                'descriptions', JSON_ARRAY(
                    CASE WHEN persona.past_gaze_failure
                         THEN '과거 보정 실패 세션은 추이에서 제외했습니다.'
                         ELSE '훈련 시선은 안정 범위에서 수집되었습니다.' END),
                'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
            ),
            'test', JSON_OBJECT(
                'status', 'AVAILABLE',
                'comparisonAvailable', TRUE,
                'points', JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 1,
                        'gazeSessionId', 150000 + persona.persona_no * 10 + 1,
                        'sourceType', 'TEST',
                        'sourceId', 141000 + persona.persona_no * 10 + 1,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:14:00'),
                            '%Y-%m-%dT%H:%i:%s'),
                        'totalVisitedDurationMs', GREATEST(22000, 68000 - persona.reading_speed * 260 - persona.trend_delta * 80),
                        'totalVisitedCount', GREATEST(38, 104 - persona.reading_speed / 2 - 6),
                        'reverseReadCount', GREATEST(1, 12 - persona.trend_delta / 20),
                        'avgVisitedDurationMs', GREATEST(380, 820 - persona.trend_delta)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 150000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TEST',
                        'sourceId', 141000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:14:00'),
                            '%Y-%m-%dT%H:%i:%s'),
                        'totalVisitedDurationMs', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
                        'totalVisitedCount', GREATEST(38, 104 - persona.reading_speed / 2 - 12),
                        'reverseReadCount', GREATEST(1, 9 - persona.trend_delta / 20),
                        'avgVisitedDurationMs', GREATEST(380, 820 - persona.trend_delta * 2)
                    )
                ),
                'changes', JSON_OBJECT(
                    'totalVisitedDurationMs', JSON_OBJECT(
                        'first', GREATEST(22000, 68000 - persona.reading_speed * 260 - persona.trend_delta * 80),
                        'latest', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
                        'delta', -persona.trend_delta * 80),
                    'totalVisitedCount', JSON_OBJECT('first', 98, 'latest', 86, 'delta', -12),
                    'reverseReadCount', JSON_OBJECT('first', 10, 'latest', 6, 'delta', -4),
                    'avgVisitedDurationMs', JSON_OBJECT(
                        'first', GREATEST(380, 820 - persona.trend_delta),
                        'latest', GREATEST(380, 820 - persona.trend_delta * 2),
                        'delta', -persona.trend_delta)
                ),
                'descriptions', JSON_ARRAY(
                    CONCAT(persona.weakness_area, ' 관련 시선 지표가 이전 검사보다 안정되었습니다.')),
                'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
            )
        )
    ),
    CONCAT('[', persona.persona_title, '] ', persona.strength_area,
           '은 강점이며 ', persona.weakness_area, '을 다음 지도 목표로 권장합니다.'),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-05-31 18:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 18:00:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM reports existing
    WHERE existing.id = 170000 + persona.persona_no * 10 + number.seq
);

INSERT INTO stories (id, student_id, story_template_id, created_at, status, progress)
SELECT
    180000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    1 + MOD(persona.persona_no + number.seq, 2),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
    END,
    CASE number.seq WHEN 1 THEN 'COMPLETED' ELSE persona.story_status END,
    CASE number.seq WHEN 1 THEN 100 ELSE persona.story_progress END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM stories existing
    WHERE existing.id = 180000 + persona.persona_no * 10 + number.seq
);

INSERT INTO story_scenes (scene_id, story_id, image_url, sequence_no, created_at)
SELECT
    181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    180000 + persona.persona_no * 10 + story_no.seq,
    NULL,
    scene_no.seq,
    TIMESTAMPADD(MINUTE, scene_no.seq * 5,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END)
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_scenes existing
    WHERE existing.scene_id =
        181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq
);

INSERT INTO story_lines
    (id, scene_id, has_choices, content, sequence_no, created_at, read_at)
SELECT
    182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + line_no.seq,
    181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    line_no.seq = 2,
    CASE line_no.seq
        WHEN 1 THEN CONCAT(persona.persona_title, '가 숲속 도서관에서 빛나는 지도를 발견했습니다.')
        ELSE CONCAT('지도에는 ', persona.strength_area, '을 활용해야 열리는 길이 그려져 있었습니다.')
    END,
    line_no.seq,
    TIMESTAMPADD(MINUTE, scene_no.seq * 5 + line_no.seq,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END),
    CASE
        WHEN story_no.seq = 1 OR persona.story_progress >= scene_no.seq * 40
        THEN TIMESTAMPADD(MINUTE, scene_no.seq * 5 + line_no.seq + 1,
            CASE story_no.seq
                WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
                ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
            END)
    END
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
JOIN demo_line_numbers line_no ON line_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_lines existing
    WHERE existing.id =
        182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + line_no.seq
);

INSERT INTO story_choices (id, story_line_id, content, created_at)
SELECT
    195000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + 2,
    CONCAT(persona.weakness_area, '을 천천히 확인하며 다음 길로 간다.'),
    TIMESTAMPADD(MINUTE, scene_no.seq * 5 + 3,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END)
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_choices existing
    WHERE existing.id = 195000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq
);

INSERT INTO word_attempt_logs
    (id, student_id, word_id, story_line_id, training_id, test_id, use_location,
     surface_text, has_audio_data, fixation_duration_ms, fixation_count,
     gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count,
     pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms,
     is_correct, created_at, total_score, question_no, target_index, token_index, is_final)
SELECT
    200000 + persona.persona_no * 100 + number.seq,
    persona.student_id,
    10000 + MOD(persona.persona_no + number.seq - 1, 10) + 1,
    NULL,
    130000 + persona.persona_no * 100 + 8,
    NULL,
    'TRAINING',
    word.content,
    TRUE,
    GREATEST(480, 1500 - persona.reading_speed * 8 + number.seq * 55),
    1 + MOD(number.seq + persona.persona_no, 4),
    (number.seq - 1) * 900,
    number.seq * 900,
    FALSE,
    CASE WHEN number.seq <= 2 AND persona.base_accuracy < 700 THEN 2 ELSE 0 END,
    LEAST(980, GREATEST(420,
        persona.base_accuracy + number.seq * 20 - CASE WHEN number.seq = 1 THEN 100 ELSE 0 END)),
    (number.seq - 1) * 850,
    number.seq * 850,
    CASE WHEN number.seq <= 2 AND persona.base_accuracy < 700 THEN FALSE ELSE TRUE END,
    TIMESTAMPADD(MINUTE, number.seq,
        TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')),
    LEAST(980, GREATEST(400, persona.base_accuracy + number.seq * 15)),
    CEIL(number.seq / 2),
    MOD(number.seq - 1, 2),
    MOD(number.seq - 1, 3),
    TRUE
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 6
JOIN words word ON word.id = 10000 + MOD(persona.persona_no + number.seq - 1, 10) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM word_attempt_logs existing
    WHERE existing.id = 200000 + persona.persona_no * 100 + number.seq
);

DROP TEMPORARY TABLE demo_features;
DROP TEMPORARY TABLE demo_line_numbers;
DROP TEMPORARY TABLE demo_scene_numbers;
DROP TEMPORARY TABLE demo_story_numbers;
DROP TEMPORARY TABLE demo_numbers;
DROP TEMPORARY TABLE demo_personas;
