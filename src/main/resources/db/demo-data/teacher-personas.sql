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
     '[관찰] 그림 단서에는 적극적으로 반응하지만 자음과 모음의 소리 연결에서 망설임이 있습니다. [지도] 한 번에 5분 이내로 기본 모음과 첫소리를 짝짓고, 성공한 글자는 그림 단서 없이 다시 확인합니다. [다음 확인] 2주 동안 정확도보다 참여 지속 시간과 독립 반응 수를 우선 기록합니다.',
     '그림 단서 활용', '자음·모음 연결', 570, 45, 34, FALSE, 'IN_PROGRESS', 35),
    (2, 2101, '꾸준히 성장하는 균형형 학습자',
     '[관찰] 음절 읽기와 문장 이해가 함께 성장하며 최근 정확도와 읽기 속도가 모두 안정적으로 상승했습니다. [지도] 현재 난이도를 유지하되 긴 문장은 의미 단위 표시 후 한 번 더 자연스럽게 읽게 합니다. [다음 확인] 2주 뒤 문장 길이를 늘려 호흡 유지 여부를 확인합니다.',
     '음절 읽기', '긴 문장 호흡', 720, 35, 58, FALSE, 'COMPLETED', 100),
    (3, 2102, '기초 검사를 마친 신규 전입 학습자',
     '[관찰] 신규 전입 후 기초 검사를 마쳤고 낱글자 인식은 양호하지만 반복 자료가 아직 적습니다. [지도] 첫 3회기는 자모·음절·짧은 낱말을 같은 비율로 제시해 기준선을 수집합니다. [다음 확인] 성급한 단계 조정 없이 오류가 반복되는 유형부터 다음 커리큘럼에 반영합니다.',
     '낱글자 인식', '학습 근거 축적', 680, 20, 42, FALSE, 'IN_PROGRESS', 20),
    (4, 2103, '전 영역을 체험한 종합 시연 학습자',
     '[관찰] 기초 음운부터 문장 유창성까지 전 영역을 경험했고 내용 이해가 가장 안정적입니다. 받침 낱말과 의미 단위 끊어 읽기에서 속도가 흔들립니다. [지도] 받침 낱말을 포함한 2~3문장 읽기를 반복하고 시선 되돌아가기를 함께 관찰합니다. [다음 확인] 2주 뒤 같은 길이의 문장으로 정확도와 호흡을 비교합니다.',
     '내용 이해', '받침·끊어 읽기', 810, 28, 72, TRUE, 'IN_PROGRESS', 60),
    (5, 2104, '시선 보정 실패를 극복한 회복형 학습자',
     '[관찰] 초기 시선 보정 실패 뒤 재측정에서는 안정적으로 수집되었고 되읽기와 이탈 횟수도 감소했습니다. [지도] 매 회기 시작 전 자세와 화면 거리를 확인한 뒤 짧은 문장에서 성공 경험을 제공합니다. [다음 확인] 보정 실패 여부와 문장 끝까지 읽은 비율을 함께 기록해 회복 추이를 유지합니다.',
     '재시도 지속성', '시선 고정 안정화', 640, 52, 49, TRUE, 'COMPLETED', 100),
    (6, 2105, '정확하지만 천천히 읽는 신중형 학습자',
     '[관찰] 정답 정확도는 높지만 낱말마다 확인 시간이 길어 문장 흐름이 자주 끊깁니다. [지도] 시간 제한은 두지 않고 동일한 짧은 문장을 세 차례 읽어 두 번째부터 묶어 읽기를 유도합니다. [다음 확인] 정확도를 유지하면서 분당 읽은 낱말 수가 완만하게 증가하는지 확인합니다.',
     '높은 정확도', '읽기 자동화 속도', 870, 18, 38, FALSE, 'IN_PROGRESS', 50),
    (7, 2106, '빠르게 읽지만 누락이 잦은 속도 우선형 학습자',
     '[관찰] 읽기 속도는 빠르지만 조사와 받침을 건너뛰어 정확도 손실이 큽니다. [지도] 문장 경계와 받침 낱말을 시각적으로 표시하고 첫 읽기는 의도적으로 속도를 낮춥니다. [다음 확인] 속도보다 누락 낱말 수가 줄어드는지를 우선 지표로 보고 표시 단서를 점차 제거합니다.',
     '빠른 읽기 속도', '낱말·받침 누락', 610, 30, 84, TRUE, 'IN_PROGRESS', 70),
    (8, 2107, '이야기 몰입도가 높은 서사형 학습자',
     '[관찰] 이야기 선택과 내용 예측에 적극적이며 맥락을 이용한 이해가 강합니다. 처음 보는 낱말은 문맥으로 추측하고 글자 단서를 놓치는 경우가 있습니다. [지도] 이야기 전후에 새 낱말 3개를 분리해 해독한 뒤 본문에서 다시 찾게 합니다. [다음 확인] 문맥 없이도 같은 낱말을 정확히 읽는지 확인합니다.',
     '이야기 이해·예측', '새 낱말 해독', 760, 32, 66, FALSE, 'COMPLETED', 100),
    (9, 2108, '비음화 발음에 집중하는 발음 교정형 학습자',
     '[관찰] 국물·읽는처럼 비음화가 포함된 낱말에서 같은 발음 오류가 반복되며 음절 분리 단서에는 잘 반응합니다. [지도] 표기와 실제 발음을 나란히 제시하고 느린 모범 발음 뒤 한 번만 재시도합니다. [다음 확인] 연습 낱말과 새 낱말에서 오류가 함께 감소하는지 구분해 기록합니다.',
     '음절 분리', '비음화 발음', 590, 42, 46, FALSE, 'IN_PROGRESS', 45),
    (10, 2109, '검사 긴장을 완화해 가는 자신감 회복형 학습자',
     '[관찰] 훈련에서는 안정적이지만 검사 화면에서 응답 시작이 늦고 정확도도 함께 낮아집니다. [지도] 검사 전 연습 문항으로 성공 경험을 제공하고 중립적인 안내만 사용합니다. [다음 확인] 검사 점수뿐 아니라 첫 응답 시간과 중단 횟수가 줄어드는지를 함께 비교합니다.',
     '훈련 참여도', '검사 상황 긴장', 670, 48, 52, TRUE, 'COMPLETED', 100),
    (11, 2110, '문장 의미 연결이 강한 이해 중심형 학습자',
     '[관찰] 중심 내용 찾기와 그림-문장 연결은 안정적이지만 소리 내어 읽을 때 문장 부호를 충분히 반영하지 않습니다. [지도] 쉼표와 마침표에서 호흡 표시를 한 뒤 의미가 달라지는 예를 비교합니다. [다음 확인] 표시 없이도 억양과 멈춤을 유지하는지 녹음 기록으로 확인합니다.',
     '중심 내용 이해', '문장 억양·호흡', 800, 24, 64, FALSE, 'IN_PROGRESS', 80),
    (12, 2111, '겹받침과 되읽기가 잦은 집중 지원형 학습자',
     '[관찰] 겹받침 낱말에서 시선 머무름과 되읽기가 많지만 재시도 안내는 잘 수용합니다. [지도] 낱말을 음절 단위로 나누고 최대 3회 안에서 마지막 시도만 평가에 반영합니다. [다음 확인] 같은 낱말의 머무름 시간과 재시도 횟수가 함께 줄어드는지 확인합니다.',
     '재시도 수용성', '겹받침·되읽기', 520, 55, 41, TRUE, 'IN_PROGRESS', 30),
    (13, 2002, '모음 구별이 빠른 시작 단계 학습자',
     '[관찰] 기본 모음 따라 쓰기와 소리 구별은 빠르게 익히지만 음절을 합칠 때 카드 순서를 바꾸는 경우가 있습니다. [지도] 모음 강점을 활용해 두 글자 음절 합성부터 시작하고 손가락으로 읽는 순서를 짚게 합니다. [다음 확인] 도움 없이 카드 순서를 유지한 문항 수와 첫 시도 성공률을 기록합니다.',
     '기본 모음 구별', '음절 합성 순서', 740, 38, 45, FALSE, 'IN_PROGRESS', 55);

CREATE TEMPORARY TABLE demo_numbers (seq INT PRIMARY KEY);
INSERT INTO demo_numbers (seq)
VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13);
CREATE TEMPORARY TABLE demo_story_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_scene_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_line_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_trend_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_token_numbers AS
SELECT seq FROM demo_numbers;

CREATE TEMPORARY TABLE demo_features AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS feature_no
FROM reading_features;
SET @demo_feature_count = (SELECT COUNT(*) FROM demo_features);

CREATE TEMPORARY TABLE demo_reading_templates AS
SELECT
    id,
    ROW_NUMBER() OVER (ORDER BY id) AS template_no
FROM training_templates
WHERE JSON_UNQUOTE(JSON_EXTRACT(prompt, '$.questionType')) IN (
    'WORD_GRID_READING',
    'SENTENCE_READING',
    'PASSAGE_READING'
);
SET @demo_reading_template_count = (SELECT COUNT(*) FROM demo_reading_templates);

UPDATE students student
JOIN demo_personas persona ON persona.student_id = student.id
SET student.teacher_memo = persona.teacher_memo,
    student.guardian = COALESCE(student.guardian, CONCAT(LEFT(student.name, 1), '보호자')),
    student.guardian_contact = COALESCE(
        student.guardian_contact,
        CONCAT('010-0000-', RIGHT(CONCAT('0000', student.id), 4))
    ),
    student.guardian_email = COALESCE(
        student.guardian_email,
        CONCAT('guardian', student.id, '@example.invalid')
    ),
    student.address = COALESCE(student.address, '서울시 데모구 읽기마을'),
    student.image_url = COALESCE(student.image_url, '/images/student-profile.png');

UPDATE daily_curriculums curriculum
JOIN demo_personas persona ON persona.student_id = curriculum.student_id
SET curriculum.status = 'IN_PROGRESS',
    curriculum.completed_at = NULL
WHERE curriculum.status = 'NOT_STARTED';

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
JOIN demo_numbers number ON number.seq <= 12
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

-- 모든 훈련 유형에 대한 페르소나별 기초 수행 기록을 제공한다.
-- 최근 커리큘럼의 동일 훈련과 비교할 수 있도록 기준 기록은 현재 회차보다 앞선다.
INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    220000 + persona.persona_no,
    persona.student_id,
    'COMPLETED',
    TIMESTAMPADD(MINUTE, persona.persona_no, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, persona.persona_no, '2026-06-24 12:30:00')
FROM demo_personas persona
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 220000 + persona.persona_no
);

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    230000 + persona.persona_no * 100 + template.id,
    template.id,
    220000 + persona.persona_no,
    template.id,
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no + 1, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no + 4, '2026-06-24 09:00:00'),
    'COMPLETED',
    JSON_OBJECT(
        'learningAssessment', CONCAT(
            persona.persona_title, '의 기초 수행 기록입니다. ',
            persona.weakness_area, ' 관련 반응은 다음 회차에서 다시 확인합니다.'
        ),
        'retryCount', CASE
            WHEN persona.base_accuracy < 600 THEN 2
            WHEN MOD(template.id + persona.persona_no, 5) = 0 THEN 1
            ELSE 0
        END,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionNumber', 1,
                'question', CONCAT(template.name, ' 기초 확인 문항'),
                'isCorrect', TRUE,
                'selectedAnswer', '첫 문항을 정확히 수행했습니다.',
                'correctAnswer', '첫 문항을 정확히 수행했습니다.'
            ),
            JSON_OBJECT(
                'questionNumber', 2,
                'question', CONCAT(persona.weakness_area, ' 확인 문항'),
                'isCorrect', MOD(template.id + persona.persona_no, 4) <> 0,
                'selectedAnswer', CASE
                    WHEN MOD(template.id + persona.persona_no, 4) <> 0
                    THEN '정확한 응답'
                    ELSE '단서를 확인한 뒤 수정함'
                END,
                'correctAnswer', '정확한 응답'
            ),
            JSON_OBJECT(
                'questionNumber', 3,
                'question', CONCAT(persona.strength_area, ' 활용 문항'),
                'isCorrect', TRUE,
                'selectedAnswer', '끝까지 수행했습니다.',
                'correctAnswer', '끝까지 수행했습니다.'
            )
        )
    ),
    LEAST(980, GREATEST(380, ROUND(
        (
            persona.base_accuracy
            + MOD(template.id * 19 + persona.persona_no * 13, 90)
            - 45
        ),
        2
    )))
FROM demo_personas persona
JOIN training_templates template
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 230000 + persona.persona_no * 100 + template.id
);

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    240000 + persona.persona_no * 100 + template.id,
    230000 + persona.persona_no * 100 + template.id,
    JSON_OBJECT(
        'version', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionId', CONCAT('baseline-', persona.persona_no, '-', template.id, '-1'),
                'questionNo', 1,
                'problem', JSON_OBJECT('targetText', CONCAT(template.name, '의 기초 문항입니다.')),
                'answer', JSON_OBJECT('correctText', '제시된 단서에 맞게 응답합니다.')
            ),
            JSON_OBJECT(
                'questionId', CONCAT('baseline-', persona.persona_no, '-', template.id, '-2'),
                'questionNo', 2,
                'problem', JSON_OBJECT(
                    'targetText', CONCAT(persona.weakness_area, '에 유의하여 읽어 보세요.')
                ),
                'answer', JSON_OBJECT('correctText', '천천히 확인하며 정확히 읽습니다.')
            )
        )
    ),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no, '2026-06-24 09:00:00')
FROM demo_personas persona
JOIN training_templates template
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 240000 + persona.persona_no * 100 + template.id
);

-- 최근 30일에 서로 다른 세 번의 읽기 기록을 만들어 정확도·읽기 속도 추이를 제공한다.
INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    250000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    'COMPLETED',
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:20:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:20:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:20:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 250000 + persona.persona_no * 10 + number.seq
);

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    251000 + persona.persona_no * 10 + number.seq,
    reading_template.id,
    250000 + persona.persona_no * 10 + number.seq,
    1,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:03:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:03:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:03:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:12:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:12:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:12:00')
    END,
    'COMPLETED',
    JSON_OBJECT(
        'learningAssessment', CONCAT(
            persona.strength_area, '을 활용한 읽기에서 ',
            CASE
                WHEN number.seq = 1 THEN CONCAT(persona.weakness_area, ' 지원이 필요했습니다.')
                WHEN number.seq = 2 THEN '첫 기록보다 망설임이 줄었습니다.'
                ELSE '정확도와 읽기 흐름이 함께 안정되었습니다.'
            END
        ),
        'retryCount', GREATEST(0, 3 - number.seq - FLOOR(persona.base_accuracy / 800)),
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionNumber', 1,
                'question', '제시된 낱말을 순서대로 읽어 보세요.',
                'isCorrect', TRUE,
                'selectedAnswer', '도서관 친구 학교',
                'correctAnswer', '도서관 친구 학교'
            ),
            JSON_OBJECT(
                'questionNumber', 2,
                'question', CONCAT(persona.weakness_area, '을 확인하는 문장을 읽어 보세요.'),
                'isCorrect', number.seq > 1 OR persona.base_accuracy >= 750,
                'selectedAnswer', CASE
                    WHEN number.seq > 1 OR persona.base_accuracy >= 750
                    THEN '친구와 함께 도서관에 갑니다.'
                    ELSE '친구와 도서관에 감니다.'
                END,
                'correctAnswer', '친구와 함께 도서관에 갑니다.'
            ),
            JSON_OBJECT(
                'questionNumber', 3,
                'question', '문장의 뜻을 짧게 말해 보세요.',
                'isCorrect', TRUE,
                'selectedAnswer', '친구와 도서관에 가는 내용입니다.',
                'correctAnswer', '친구와 도서관에 가는 내용입니다.'
            )
        )
    ),
    LEAST(980, GREATEST(400, ROUND(
        persona.base_accuracy + (number.seq - 1) * persona.trend_delta,
        2
    )))
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN demo_reading_templates reading_template
  ON reading_template.template_no =
     1 + MOD(persona.persona_no + number.seq - 2, @demo_reading_template_count)
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 251000 + persona.persona_no * 10 + number.seq
);

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    252000 + persona.persona_no * 10 + number.seq,
    251000 + persona.persona_no * 10 + number.seq,
    JSON_OBJECT(
        'version', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionId', CONCAT('trend-', persona.persona_no, '-', number.seq, '-1'),
                'questionNo', 1,
                'problem', JSON_OBJECT('targetText', '도서관 친구 학교'),
                'answer', JSON_OBJECT('correctText', '도서관 친구 학교')
            ),
            JSON_OBJECT(
                'questionId', CONCAT('trend-', persona.persona_no, '-', number.seq, '-2'),
                'questionNo', 2,
                'problem', JSON_OBJECT('targetText', '친구와 함께 도서관에 갑니다.'),
                'answer', JSON_OBJECT('correctText', '친구와 함께 도서관에 갑니다.')
            )
        )
    ),
    training.created_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + number.seq
JOIN training_templates template ON template.id = training.training_template_id
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 252000 + persona.persona_no * 10 + number.seq
);

INSERT INTO word_attempt_logs
    (id, student_id, word_id, story_line_id, training_id, test_id, use_location,
     surface_text, has_audio_data, fixation_duration_ms, fixation_count,
     gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count,
     pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms,
     is_correct, created_at, total_score, question_no, target_index, token_index, is_final)
SELECT
    300000 + persona.persona_no * 100 + trend_no.seq * 10 + token_no.seq,
    persona.student_id,
    word.id,
    NULL,
    251000 + persona.persona_no * 10 + trend_no.seq,
    NULL,
    'TRAINING',
    word.content,
    TRUE,
    GREATEST(
        260,
        1150 - persona.reading_speed * 6 - (trend_no.seq - 1) * persona.trend_delta * 2
            + token_no.seq * 18
    ),
    CASE
        WHEN persona.weakness_area LIKE '%되읽기%' AND token_no.seq <= 2 THEN 3
        WHEN persona.weakness_area LIKE '%누락%' AND token_no.seq = 3 THEN 1
        ELSE 1 + MOD(token_no.seq + persona.persona_no, 2)
    END,
    FLOOR(
        (token_no.seq - 1) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FLOOR(
        (token_no.seq - 0.25) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FALSE,
    CASE
        WHEN token_no.seq <= 2 AND persona.base_accuracy < 650 THEN 2
        WHEN persona.past_gaze_failure AND trend_no.seq = 1 AND token_no.seq = 3 THEN 1
        ELSE 0
    END,
    LEAST(980, GREATEST(
        420,
        persona.base_accuracy + (trend_no.seq - 1) * persona.trend_delta
            + token_no.seq * 8 - 35
    )),
    FLOOR(
        (token_no.seq - 1) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FLOOR(
        (token_no.seq - 0.25) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    NOT (
        token_no.seq = 2
        AND trend_no.seq = 1
        AND persona.base_accuracy < 700
    ),
    TIMESTAMPADD(
        SECOND,
        token_no.seq * 20,
        training.started_at
    ),
    LEAST(980, GREATEST(
        400,
        persona.base_accuracy + (trend_no.seq - 1) * persona.trend_delta
            + token_no.seq * 7 - 30
    )),
    CEIL(token_no.seq / 2),
    MOD(token_no.seq - 1, 2),
    token_no.seq - 1,
    TRUE
FROM demo_personas persona
JOIN demo_trend_numbers trend_no ON trend_no.seq <= 3
JOIN demo_token_numbers token_no ON token_no.seq <= 6
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + trend_no.seq
JOIN words word ON word.id = 10000 + MOD(persona.persona_no + token_no.seq - 1, 10) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM word_attempt_logs existing
    WHERE existing.id =
        300000 + persona.persona_no * 100 + trend_no.seq * 10 + token_no.seq
);

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    260000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    NULL,
    251000 + persona.persona_no * 10 + number.seq,
    NULL,
    'TRAINING',
    training.started_at,
    training.finished_at,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.22 + persona.persona_no * 0.01, 'y', 0.39),
        JSON_OBJECT('timestampMs', 240, 'x', 0.41, 'y', 0.42),
        JSON_OBJECT('timestampMs', 480, 'x', 0.57, 'y', 0.44)
    ),
    CASE
        WHEN number.seq = 1 AND persona.past_gaze_failure THEN 'FAILED'
        ELSE 'COMPLETED'
    END,
    CASE
        WHEN number.seq = 1 AND persona.past_gaze_failure THEN 'FAILED'
        ELSE 'SUCCESS'
    END,
    training.started_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + number.seq
WHERE NOT EXISTS (
    SELECT 1 FROM gaze_sessions existing
    WHERE existing.id = 260000 + persona.persona_no * 10 + number.seq
);

INSERT INTO gaze_analysis_results
    (id, gaze_session_id, total_visited_duration, total_visited_count,
     reverse_read_count, avg_visited_duration, created_at)
SELECT
    261000 + persona.persona_no * 10 + number.seq,
    260000 + persona.persona_no * 10 + number.seq,
    GREATEST(
        18000,
        61000 - persona.reading_speed * 250 - number.seq * persona.trend_delta * 75
    ),
    GREATEST(30, 96 - persona.reading_speed / 2 - number.seq * 5),
    GREATEST(
        0,
        12 - number.seq * 2
            + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
    ),
    GREATEST(300, 780 - persona.trend_delta * number.seq),
    TIMESTAMPADD(MINUTE, 1, session.ended_at)
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN gaze_sessions session ON session.id = 260000 + persona.persona_no * 10 + number.seq
WHERE session.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM gaze_analysis_results existing
      WHERE existing.id = 261000 + persona.persona_no * 10 + number.seq
  );

-- 교수자가 어떤 완료 훈련을 먼저 열어도 시선 분석 예시를 확인할 수 있게 누락 세션을 보강한다.
INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    400000 + training.id,
    persona.student_id,
    NULL,
    training.id,
    NULL,
    'TRAINING',
    training.started_at,
    training.finished_at,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.18 + persona.persona_no * 0.01, 'y', 0.38),
        JSON_OBJECT('timestampMs', 260, 'x', 0.39, 'y', 0.41),
        JSON_OBJECT('timestampMs', 520, 'x', 0.61, 'y', 0.43)
    ),
    'COMPLETED',
    'SUCCESS',
    training.started_at
FROM demo_personas persona
JOIN daily_curriculums curriculum ON curriculum.student_id = persona.student_id
JOIN trainings training ON training.daily_curriculum_id = curriculum.id
WHERE training.status = 'COMPLETED'
  AND training.started_at IS NOT NULL
  AND training.finished_at IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM gaze_sessions existing
      WHERE existing.training_id = training.id
        AND existing.content_type = 'TRAINING'
  )
  AND NOT EXISTS (
      SELECT 1 FROM gaze_sessions existing
      WHERE existing.id = 400000 + training.id
  );

INSERT INTO gaze_analysis_results
    (id, gaze_session_id, total_visited_duration, total_visited_count,
     reverse_read_count, avg_visited_duration, created_at)
SELECT
    500000 + training.id,
    session.id,
    GREATEST(18000, 64000 - persona.reading_speed * 260),
    GREATEST(28, 94 - persona.reading_speed / 2),
    GREATEST(
        0,
        10
            + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
            - FLOOR(COALESCE(training.accuracy, 60) / 12)
    ),
    GREATEST(280, 820 - persona.trend_delta * 2),
    TIMESTAMPADD(MINUTE, 1, session.ended_at)
FROM demo_personas persona
JOIN daily_curriculums curriculum ON curriculum.student_id = persona.student_id
JOIN trainings training ON training.daily_curriculum_id = curriculum.id
JOIN gaze_sessions session
  ON session.id = 400000 + training.id
WHERE session.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM gaze_analysis_results existing
      WHERE existing.gaze_session_id = session.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM gaze_analysis_results existing
      WHERE existing.id = 500000 + training.id
  );

-- DB 정수 저장 단위(0~1000)를 화면 백분율(0~100)로 잘못 넣은 기존 데모 행을 복구한다.
UPDATE trainings training
JOIN daily_curriculums curriculum ON curriculum.id = training.daily_curriculum_id
JOIN demo_personas persona ON persona.student_id = curriculum.student_id
SET training.accuracy = ROUND(training.accuracy * 10)
WHERE training.accuracy BETWEEN 0 AND 100;

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
        'changeFromPrevious', CASE
            WHEN number.seq = 1 THEN NULL
            ELSE ROUND(persona.trend_delta / 10, 1)
        END,
        'strengthAreas', JSON_ARRAY(persona.strength_area),
        'improvementAreas', JSON_ARRAY(persona.weakness_area),
        'recommendedCourse', CONCAT(persona.weakness_area, ' 집중 훈련'),
        'nextTestRecommendation', CONCAT(
            '2주 후 ', persona.weakness_area, ' 영역의 같은 난이도 재검사를 권장합니다.'
        ),
        'areaScores', JSON_ARRAY(
            JSON_OBJECT(
                'area', persona.strength_area,
                'score', LEAST(98, ROUND(
                    (persona.base_accuracy + 90 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                ))
            ),
            JSON_OBJECT(
                'area', persona.weakness_area,
                'score', LEAST(95, GREATEST(35, ROUND(
                    (persona.base_accuracy - 110 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                )))
            ),
            JSON_OBJECT(
                'area', '읽기 유창성',
                'score', LEAST(98, GREATEST(35, persona.reading_speed + number.seq * 3))
            )
        ),
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

UPDATE tests test
JOIN demo_personas persona
JOIN demo_numbers number
  ON number.seq <= 3
 AND test.id = 141000 + persona.persona_no * 10 + number.seq
SET test.result = JSON_SET(
        COALESCE(test.result, JSON_OBJECT()),
        '$.overallScore', LEAST(98, GREATEST(45, ROUND(
            (persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10
        ))),
        '$.changeFromPrevious', CASE
            WHEN number.seq = 1 THEN NULL
            ELSE ROUND(persona.trend_delta / 10, 1)
        END,
        '$.strengthAreas', JSON_ARRAY(persona.strength_area),
        '$.improvementAreas', JSON_ARRAY(persona.weakness_area),
        '$.recommendedCourse', CONCAT(persona.weakness_area, ' 집중 훈련'),
        '$.nextTestRecommendation', CONCAT(
            '2주 후 ', persona.weakness_area, ' 영역의 같은 난이도 재검사를 권장합니다.'
        ),
        '$.areaScores', JSON_ARRAY(
            JSON_OBJECT(
                'area', persona.strength_area,
                'score', LEAST(98, ROUND(
                    (persona.base_accuracy + 90 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                ))
            ),
            JSON_OBJECT(
                'area', persona.weakness_area,
                'score', LEAST(95, GREATEST(35, ROUND(
                    (persona.base_accuracy - 110 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                )))
            ),
            JSON_OBJECT(
                'area', '읽기 유창성',
                'score', LEAST(98, GREATEST(35, persona.reading_speed + number.seq * 3))
            )
        )
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

UPDATE reports report
JOIN demo_personas persona ON report.student_id = persona.student_id
SET report.teacher_memo = CONCAT(
        '[기간 관찰] ', persona.strength_area, '은 안정적으로 활용했습니다. ',
        persona.weakness_area, '에서는 단서 제공 여부에 따라 수행 차이가 나타났습니다. ',
        '[다음 지도] 현재 난이도를 유지하며 같은 유형을 짧게 반복하고, 2주 뒤 독립 수행을 비교합니다.'
    ),
    report.snapshot_data = JSON_SET(
        report.snapshot_data,
        '$.gazeTrend.training',
        JSON_OBJECT(
            'status', 'AVAILABLE',
            'comparisonAvailable', TRUE,
            'points', CASE
                WHEN persona.past_gaze_failure THEN JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 2,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-14 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 2 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 10),
                        'reverseReadCount', GREATEST(
                            0,
                            8 + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 2)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 3,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 3,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-24 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                        'reverseReadCount', GREATEST(
                            0,
                            6 + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 3)
                    )
                )
                ELSE JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 1,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 1,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 1,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-04 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 5),
                        'reverseReadCount', GREATEST(
                            0,
                            10 + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 2,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-14 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 2 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 10),
                        'reverseReadCount', GREATEST(
                            0,
                            8 + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 2)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 3,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 3,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-24 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                        'reverseReadCount', GREATEST(
                            0,
                            6 + CASE WHEN persona.weakness_area LIKE '%되읽기%' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 3)
                    )
                )
            END,
            'changes', JSON_OBJECT(
                'totalVisitedDurationMs', JSON_OBJECT(
                    'first', GREATEST(
                        18000,
                        61000 - persona.reading_speed * 250
                            - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                              * persona.trend_delta * 75
                    ),
                    'latest', GREATEST(
                        18000,
                        61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                    ),
                    'delta', -(
                        3 - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                    ) * persona.trend_delta * 75
                ),
                'totalVisitedCount', JSON_OBJECT(
                    'first', GREATEST(
                        30,
                        96 - persona.reading_speed / 2
                            - CASE WHEN persona.past_gaze_failure THEN 10 ELSE 5 END
                    ),
                    'latest', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                    'delta', CASE WHEN persona.past_gaze_failure THEN -5 ELSE -10 END
                ),
                'reverseReadCount', JSON_OBJECT(
                    'first', CASE WHEN persona.past_gaze_failure THEN 8 ELSE 10 END,
                    'latest', 6,
                    'delta', CASE WHEN persona.past_gaze_failure THEN -2 ELSE -4 END
                ),
                'avgVisitedDurationMs', JSON_OBJECT(
                    'first', GREATEST(
                        300,
                        780 - persona.trend_delta
                            * CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                    ),
                    'latest', GREATEST(300, 780 - persona.trend_delta * 3),
                    'delta', -persona.trend_delta
                        * (3 - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END)
                )
            ),
            'descriptions', JSON_ARRAY(
                CONCAT(
                    persona.weakness_area,
                    ' 지도 뒤 훈련 중 머무름과 되읽기 지표가 완만하게 감소했습니다.'
                ),
                CASE
                    WHEN persona.past_gaze_failure
                    THEN '첫 회기 보정 실패는 비교에서 제외하고 이후 성공 세션만 반영했습니다.'
                    ELSE '세 회기 모두 보정에 성공해 같은 조건의 추이를 비교했습니다.'
                END
            ),
            'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
        )
    )
WHERE report.id BETWEEN 170000 AND 170999;

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

DROP TEMPORARY TABLE demo_reading_templates;
DROP TEMPORARY TABLE demo_features;
DROP TEMPORARY TABLE demo_token_numbers;
DROP TEMPORARY TABLE demo_trend_numbers;
DROP TEMPORARY TABLE demo_line_numbers;
DROP TEMPORARY TABLE demo_scene_numbers;
DROP TEMPORARY TABLE demo_story_numbers;
DROP TEMPORARY TABLE demo_numbers;
DROP TEMPORARY TABLE demo_personas;
