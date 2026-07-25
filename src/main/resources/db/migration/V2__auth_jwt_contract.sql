ALTER TABLE `teachers`
	ADD COLUMN `login_id` varchar(50) NULL AFTER `id`;

UPDATE `teachers`
SET `login_id` = `email`
WHERE `login_id` IS NULL;

ALTER TABLE `teachers`
	MODIFY COLUMN `login_id` varchar(50) NOT NULL,
	ADD CONSTRAINT `UK_TEACHERS_LOGIN_ID` UNIQUE (`login_id`);

CREATE TABLE `auth_refresh_sessions` (
	`id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`teacher_id` bigint NOT NULL,
	`student_id` bigint NULL,
	`audience` varchar(30) NOT NULL COMMENT 'ADMIN or LEARNING',
	`token_hash` char(64) NOT NULL,
	`expires_at` timestamp NOT NULL,
	`revoked_at` timestamp NULL,
	`created_at` timestamp NOT NULL,
	CONSTRAINT `UK_AUTH_REFRESH_SESSIONS_TOKEN_HASH` UNIQUE (`token_hash`),
	CONSTRAINT `FK_AUTH_REFRESH_SESSIONS_TEACHER`
		FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
	CONSTRAINT `FK_AUTH_REFRESH_SESSIONS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
);

CREATE TABLE `auth_revoked_access_tokens` (
	`token_id` char(36) NOT NULL PRIMARY KEY,
	`expires_at` timestamp NOT NULL,
	`revoked_at` timestamp NOT NULL
);
