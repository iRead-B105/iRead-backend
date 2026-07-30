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
