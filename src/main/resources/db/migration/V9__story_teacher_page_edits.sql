ALTER TABLE `story_lines`
    ADD COLUMN `revision` bigint NOT NULL DEFAULT 0 AFTER `read_at`;

CREATE TABLE `story_page_edit_audits` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `story_line_id` bigint NOT NULL,
    `teacher_id` bigint NOT NULL,
    `edit_type` varchar(30) NOT NULL,
    `before_value` json NULL,
    `after_value` json NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `PK_STORY_PAGE_EDIT_AUDITS` PRIMARY KEY (`id`),
    CONSTRAINT `FK_STORY_PAGE_EDIT_AUDITS_LINE`
        FOREIGN KEY (`story_line_id`) REFERENCES `story_lines` (`id`),
    CONSTRAINT `FK_STORY_PAGE_EDIT_AUDITS_TEACHER`
        FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
    INDEX `IDX_STORY_PAGE_EDIT_AUDITS_LINE_CREATED` (`story_line_id`, `created_at`)
);
