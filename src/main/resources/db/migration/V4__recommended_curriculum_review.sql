ALTER TABLE `daily_curriculums`
    ADD COLUMN `review_status` varchar(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN `reviewed_at` timestamp NULL,
    ADD COLUMN `reviewed_by_teacher_id` bigint NULL,
    ADD CONSTRAINT `FK_DAILY_CURRICULUMS_REVIEWED_BY_TEACHER`
        FOREIGN KEY (`reviewed_by_teacher_id`) REFERENCES `teachers` (`id`);

UPDATE `daily_curriculums`
SET `review_status` = 'GENERATION_PENDING'
WHERE `source_test_curriculum_id` IS NOT NULL;
