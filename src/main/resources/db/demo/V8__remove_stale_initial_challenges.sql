-- Legacy demo students already have completed assessments and learning history.
-- Remove only the obsolete initial challenges created by the original demo seed.
DELETE FROM `gaze_analysis_results`
WHERE `gaze_session_id` IN (
    SELECT `id`
    FROM `gaze_sessions`
    WHERE `test_id` IN (
        SELECT `id` FROM `tests` WHERE `test_curriculum_id` IN (5001, 5002)
    )
);

DELETE FROM `gaze_sessions`
WHERE `test_id` IN (
    SELECT `id` FROM `tests` WHERE `test_curriculum_id` IN (5001, 5002)
);

DELETE FROM `word_attempt_logs`
WHERE `test_id` IN (
    SELECT `id` FROM `tests` WHERE `test_curriculum_id` IN (5001, 5002)
);

DELETE FROM `test_datas`
WHERE `test_id` IN (
    SELECT `id` FROM `tests` WHERE `test_curriculum_id` IN (5001, 5002)
);

DELETE FROM `tests` WHERE `test_curriculum_id` IN (5001, 5002);
DELETE FROM `test_curriculums` WHERE `id` IN (5001, 5002);
