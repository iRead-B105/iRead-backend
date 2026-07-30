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
