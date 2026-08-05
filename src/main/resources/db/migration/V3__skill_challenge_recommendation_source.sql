ALTER TABLE `test_curriculums`
    ADD COLUMN `recommendation_status` varchar(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN `recommendation_error` varchar(2000) NULL,
    ADD COLUMN `recommendation_last_attempt_at` timestamp NULL,
    ADD COLUMN `recommendation_retry_count` int NOT NULL DEFAULT 0;

ALTER TABLE `daily_curriculums`
    ADD COLUMN `source_test_curriculum_id` bigint NULL,
    ADD CONSTRAINT `UQ_DAILY_CURRICULUMS_SOURCE_TEST`
        UNIQUE (`source_test_curriculum_id`),
    ADD CONSTRAINT `FK_DAILY_CURRICULUMS_SOURCE_TEST`
        FOREIGN KEY (`source_test_curriculum_id`) REFERENCES `test_curriculums` (`id`);
