-- Teacher persona fixtures predate the learner question contract. Normalize existing
-- rows so the same data can be opened through the learner training and test APIs.
UPDATE `training_datas`
SET `generated_data` = JSON_OBJECT(
    'schemaVersion', 2,
    'trainingType', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.trainingType')),
    'personaFocus', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.personaFocus')),
    'questions', JSON_ARRAY(
        JSON_OBJECT(
            'questionId', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.questions[0].questionId')),
            'questionNo', 1,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('국물')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '국물')),
            'answer', JSON_OBJECT('expectedText', '국물')
        ),
        JSON_OBJECT(
            'questionId', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.questions[1].questionId')),
            'questionNo', 2,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '도서관에', '갑니다.')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 도서관에 갑니다.')),
            'answer', JSON_OBJECT('expectedText', '친구와 도서관에 갑니다.')
        ),
        JSON_OBJECT(
            'questionId', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.questions[2].questionId')),
            'questionNo', 3,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '함께', '책을', '읽는', '내용입니다.')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 함께 책을 읽는 내용입니다.')),
            'answer', JSON_OBJECT('expectedText', '친구와 함께 책을 읽는 내용입니다.')
        )
    )
)
WHERE `id` BETWEEN 135101 AND 136212
  AND JSON_EXTRACT(`generated_data`, '$.questions[0].problem.targetText') IS NOT NULL;

UPDATE `test_datas`
SET `generated_data` = JSON_OBJECT(
    'schemaVersion', 2,
    'personaFocus', JSON_UNQUOTE(JSON_EXTRACT(`generated_data`, '$.personaFocus')),
    'questions', JSON_ARRAY(
        JSON_OBJECT(
            'questionNo', 1,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('도서관에서', '책을', '읽었습니다.')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '도서관에서 책을 읽었습니다.')),
            'answer', JSON_OBJECT('expectedText', '도서관에서 책을 읽었습니다.')
        ),
        JSON_OBJECT(
            'questionNo', 2,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('친구와', '함께', '길을', '찾았습니다.')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '친구와 함께 길을 찾았습니다.')),
            'answer', JSON_OBJECT('expectedText', '친구와 함께 길을 찾았습니다.')
        ),
        JSON_OBJECT(
            'questionNo', 3,
            'type', 'SENTENCE_READING',
            'requiredInputs', JSON_ARRAY('VOICE'),
            'content', JSON_OBJECT('tokens', JSON_ARRAY('서로', '도와', '문제를', '해결했습니다.')),
            'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '서로 도와 문제를 해결했습니다.')),
            'answer', JSON_OBJECT('expectedText', '서로 도와 문제를 해결했습니다.')
        )
    )
)
WHERE `id` BETWEEN 145011 AND 145123
  AND JSON_EXTRACT(`generated_data`, '$.questions[0].problem.targetText') IS NOT NULL;
