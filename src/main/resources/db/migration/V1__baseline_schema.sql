CREATE TABLE `training_templates` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`curriculum_unit_id` bigint NOT NULL,
	`name` varchar(100) NOT NULL,
	`prompt` text NOT NULL,
	`sequence_no` int NOT NULL,
	CONSTRAINT `PK_TRAINING_TEMPLATES` PRIMARY KEY (`id`)
);

CREATE TABLE `students` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`teacher_id` bigint NOT NULL,
	`name` varchar(10) NOT NULL COMMENT '실명',
	`birthday` date NULL,
	`gender` varchar(10) NULL COMMENT 'Enum: boy, girl',
	`school` varchar(20) NULL,
	`guardian` varchar(10) NULL,
	`guardian_contact` varchar(20) NULL,
	`guardian_email` varchar(50) NULL,
	`address` varchar(100) NULL,
	`created_at` timestamp NOT NULL COMMENT '생성일',
	`image_url` varchar(255) NULL,
	`teacher_memo` text NULL,
	CONSTRAINT `PK_STUDENTS` PRIMARY KEY (`id`)
);

CREATE TABLE `reading_features` (
	`id` bigint NOT NULL,
	`parent_feature_id` bigint NULL,
	`feature_code` varchar(150) NOT NULL,
	`feature_name` varchar(150) NOT NULL,
	`category` varchar(30) NOT NULL COMMENT 'Enum: GRAPHEME, SYLLABLE, PHONOLOGY, WORD, SENTENCE',
	`scope` varchar(30) NOT NULL COMMENT 'Enum: CHARACTER, SYLLABLE, WORD, WORD_BOUNDARY, SENTENCE',
	`created_at` timestamp NULL,
	CONSTRAINT `PK_READING_FEATURES` PRIMARY KEY (`id`)
);

CREATE TABLE `student_feature_profiles` (
	`id` bigint NOT NULL,
	`student_id` bigint NOT NULL,
	`reading_features_id` bigint NOT NULL,
	`accuracy_rate` decimal(5,4) NULL COMMENT '0~1',
	`avg_pronunciation_scor` int NULL COMMENT '0~1000',
	`pronunciation_error_rate` decimal(8,2) NULL,
	`avg_fixation_duration_ms` int NULL,
	`avg_fixation_count` decimal(8,2) NULL,
	`avg_regression_count` decimal(8,2) NULL,
	`skip_rate` decimal(5,2) NULL COMMENT '0~1',
	`avg_reading_time_ms` int NULL,
	`weakness_score` int NULL COMMENT '0~1000',
	`confidence` decimal(5,4) NOT NULL COMMENT '0~1',
	`evidence_count` int NULL,
	`last_evidence_at` timestamp NULL,
	`analyzed_at` timestamp NULL,
	CONSTRAINT `PK_STUDENT_FEATURE_PROFILES` PRIMARY KEY (`id`)
);

CREATE TABLE `auth_refresh_sessions` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`teacher_id` bigint NOT NULL,
	`student_id` bigint NULL,
	`audience` varchar(30) NOT NULL COMMENT 'ADMIN or LEARNING',
	`token_hash` char(64) NOT NULL,
	`expires_at` timestamp NOT NULL,
	`revoked_at` timestamp NULL,
	`created_at` timestamp NOT NULL,
	CONSTRAINT `PK_AUTH_REFRESH_SESSIONS` PRIMARY KEY (`id`)
);

CREATE TABLE `password_reset_tokens` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`teacher_id` bigint NOT NULL,
	`token_hash` char(64) NOT NULL COMMENT 'SHA-256 hex digest',
	`expires_at` timestamp NOT NULL,
	`used_at` timestamp NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_PASSWORD_RESET_TOKENS` PRIMARY KEY (`id`)
);

CREATE TABLE `curriculum_units` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`unit_name` varchar(50) NOT NULL COMMENT '파닉스, 단어 etc',
	`sequence_no` int NULL,
	CONSTRAINT `PK_CURRICULUM_UNITS` PRIMARY KEY (`id`)
);

CREATE TABLE `story_templates` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`title` varchar(50) NOT NULL,
	`content` text NOT NULL COMMENT 'AI에게 제공하는 이야기 컨텍스트',
	`image_url` varchar(255) NULL,
	CONSTRAINT `PK_STORY_TEMPLATES` PRIMARY KEY (`id`)
);

CREATE TABLE `story_scenes` (
	`scene_id` bigint NOT NULL AUTO_INCREMENT,
	`story_id` bigint NOT NULL,
	`image_url` varchar(255) NULL,
	`sequence_no` int NOT NULL,
	`created_at` timestamp NOT NULL,
	CONSTRAINT `PK_STORY_SCENES` PRIMARY KEY (`scene_id`)
);

CREATE TABLE `word_categories` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`word_id` bigint NOT NULL,
	`category_name` varchar(50) NOT NULL COMMENT 'Enum: 받침없는단어, ㅆ받침단어 등',
	CONSTRAINT `PK_WORD_CATEGORIES` PRIMARY KEY (`id`)
);

CREATE TABLE `training_datas` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`train_id` bigint NOT NULL,
	`generated_data` json NULL COMMENT '훈련에서 쓰일 AI 생성 단어 또는 문장',
	`created_at` timestamp NULL,
	CONSTRAINT `PK_TRAINING_DATAS` PRIMARY KEY (`id`),
	CONSTRAINT `UK_TRAINING_DATAS_TRAIN_ID` UNIQUE (`train_id`)
);

CREATE TABLE `stories` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`story_template_id` bigint NOT NULL,
	`created_at` timestamp NOT NULL,
	`status` varchar(30) NOT NULL COMMENT 'Enum: IN_PROGRESS, COMPLETED, DELETED',
	`progress` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '이야기 진행률 0~100',
	CONSTRAINT `PK_STORIES` PRIMARY KEY (`id`),
	CONSTRAINT `CHK_STORIES_PROGRESS`
		CHECK (`progress` BETWEEN 0 AND 100)
);

CREATE TABLE `gaze_analysis_results` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`gaze_session_id` bigint NOT NULL,
	`total_visited_duration` int NOT NULL,
	`total_visited_count` int NOT NULL,
	`reverse_read_count` int NOT NULL,
	`avg_visited_duration` int NULL,
	`sentence_metrics` json NULL,
	`regressions` json NULL,
	`analysis_meta` json NULL,
	`created_at` timestamp NOT NULL,
	CONSTRAINT `PK_GAZE_ANALYSIS_RESULTS` PRIMARY KEY (`id`)
);

CREATE TABLE `gaze_sessions` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`test_id` bigint NULL,
	`training_id` bigint NULL,
	`story_id` bigint NULL,
	`content_type` varchar(20) NOT NULL COMMENT 'TEST, TRAINING, STORY',
	`started_at` timestamp NOT NULL,
	`ended_at` timestamp NULL,
	`data_url` varchar(255) NULL COMMENT '초당 5~10프레임 수집 데이터를 저장한 파일 URL',
	`status` varchar(20) NOT NULL COMMENT 'READY, RUNNING, COMPLETED, FAILED',
	`calibration_status` varchar(20) NOT NULL COMMENT 'NOT_STARTED, SUCCESS, FAILED, SKIPPED',
	`created_at` timestamp NOT NULL,
	CONSTRAINT `PK_GAZE_SESSIONS` PRIMARY KEY (`id`)
);

CREATE TABLE `words` (
	`id` bigint NOT NULL AUTO_INCREMENT COMMENT '단어 기본형',
	`content` varchar(50) NOT NULL,
	`length` int NOT NULL,
	CONSTRAINT `PK_WORDS` PRIMARY KEY (`id`)
);

CREATE TABLE `reports` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`start_date` timestamp NOT NULL COMMENT '리포트 기간 시작',
	`end_date` timestamp NOT NULL COMMENT '리포트 기간 종료',
	`snapshot_data` json NULL,
	`teacher_memo` text NULL,
	`created_at` timestamp NOT NULL COMMENT '생성일',
	CONSTRAINT `PK_REPORTS` PRIMARY KEY (`id`),
	CONSTRAINT `UQ_REPORTS_STUDENT_PERIOD` UNIQUE (`student_id`, `start_date`, `end_date`)
);

CREATE TABLE `teachers` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`email` varchar(50) NOT NULL,
	`password` varchar(100) NOT NULL,
	`name` varchar(10) NOT NULL COMMENT '실명',
	`organization` varchar(100) NULL,
	`created_at` timestamp NOT NULL COMMENT '생성일',
	`gender` varchar(10) NULL COMMENT 'Enum',
	`image_url` varchar(255) NULL,
	CONSTRAINT `PK_TEACHERS` PRIMARY KEY (`id`)
);

CREATE TABLE `story_lines` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`scene_id` bigint NOT NULL,
	`has_choices` boolean NOT NULL,
	`content` json NOT NULL COMMENT '대사 본문과 형태소·G2P 분석 결과',
	`sequence_no` int NOT NULL,
	`created_at` timestamp NOT NULL,
	`read_at` timestamp NULL,
	CONSTRAINT `PK_STORY_LINES` PRIMARY KEY (`id`)
);

CREATE TABLE `word_attempt_logs` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`word_id` bigint NOT NULL COMMENT '단어 기본형',
	`story_line_id` bigint NULL,
	`training_id` bigint NULL,
	`test_id` bigint NULL,
	`use_location` varchar(10) NOT NULL COMMENT 'Enum: TEST, TRAINING, STORY',
	`surface_text` varchar(50) NULL COMMENT '문장 안에서 사용된 단어 형태',
	`has_gaze_data` boolean NOT NULL DEFAULT false COMMENT '시선 사용 여부',
	`has_audio_data` boolean NOT NULL,
	`fixation_duration_ms` int NULL,
	`fixation_count` int NULL,
	`gaze_start_offset_ms` int NULL,
	`gaze_end_offset_ms` int NULL,
	`is_skipped` boolean NULL,
	`regression_count` int NULL,
	`pronunciation_accuracy_score` int NULL COMMENT 'Azure 단어별 AccuracyScore x 10 (0~1000)',
	`speech_start_offset_ms` int NULL,
	`speech_end_offset_ms` int NULL,
	`is_correct` boolean NULL,
	`created_at` timestamp NULL,
	`total_score` int NULL COMMENT '발음·시선·읽기 수행 종합 단어 점수 0~1000',
	`question_no` int NULL COMMENT '1부터 시작',
	`target_index` int NULL COMMENT '0부터 시작',
	`token_index` int NULL COMMENT '0부터 시작',
	`is_final` boolean NOT NULL DEFAULT true COMMENT '같은 문항·대상·토큰 위치의 최종 시도',
	CONSTRAINT `PK_WORD_ATTEMPT_LOGS` PRIMARY KEY (`id`),
	CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_PRONUNCIATION_ACCURACY_SCORE`
		CHECK (`pronunciation_accuracy_score` IS NULL
			OR `pronunciation_accuracy_score` BETWEEN 0 AND 1000),
	CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_TOTAL_SCORE`
		CHECK (`total_score` BETWEEN 0 AND 1000),
	CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_QUESTION_NO`
		CHECK (`question_no` IS NULL OR `question_no` >= 1),
	CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_TARGET_INDEX`
		CHECK (`target_index` IS NULL OR `target_index` >= 0),
	CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_TOKEN_INDEX`
		CHECK (`token_index` IS NULL OR `token_index` >= 0)
);

CREATE TABLE `characters` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`story_id` bigint NOT NULL,
	`image_url` text NULL,
	`created_at` timestamp NOT NULL,
	`name` varchar(50) NULL,
	CONSTRAINT `PK_CHARACTER` PRIMARY KEY (`id`)
);

CREATE TABLE `story_choices` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`story_line_id` bigint NOT NULL,
	`content` text NOT NULL,
	`created_at` timestamp NOT NULL,
	CONSTRAINT `PK_STORY_CHOICES` PRIMARY KEY (`id`)
);

CREATE TABLE `trainings` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`training_template_id` bigint NOT NULL,
	`daily_curriculum_id` bigint NOT NULL,
	`sequence_no` int NOT NULL COMMENT '커리큘럼 내 훈련 순서',
	`created_at` timestamp NOT NULL,
	`started_at` timestamp NULL,
	`finished_at` timestamp NULL,
	`status` varchar(20) NOT NULL COMMENT 'Enum: NOT_READY, NOT_STARTED, IN_PROGRESS, COMPLETED',
	`result` json NULL COMMENT '문항별 정답 여부와 오답 부분',
	`accuracy` int NULL COMMENT 'AI가 판단한 훈련 정확도 0~1000',
	CONSTRAINT `PK_TRAININGS` PRIMARY KEY (`id`),
	CONSTRAINT `CHK_TRAININGS_ACCURACY`
		CHECK (`accuracy` BETWEEN 0 AND 1000)
);

CREATE TABLE `daily_curriculums` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`student_id` bigint NOT NULL,
	`status` varchar(20) NOT NULL COMMENT 'Enum: NOT_STARTED, IN_PROGRESS, COMPLETED',
	`created_at` timestamp NOT NULL,
	`completed_at` timestamp NULL,
	`not_started_student_id` bigint
		GENERATED ALWAYS AS (
			CASE WHEN `status` = 'NOT_STARTED' THEN `student_id` ELSE NULL END
		) STORED,
	`in_progress_student_id` bigint
		GENERATED ALWAYS AS (
			CASE WHEN `status` = 'IN_PROGRESS' THEN `student_id` ELSE NULL END
		) STORED,
	CONSTRAINT `PK_DAILY_CURRICULUMS` PRIMARY KEY (`id`),
	CONSTRAINT `UQ_DAILY_CURRICULUMS_NOT_STARTED_STUDENT`
		UNIQUE (`not_started_student_id`),
	CONSTRAINT `UQ_DAILY_CURRICULUMS_IN_PROGRESS_STUDENT`
		UNIQUE (`in_progress_student_id`)
);

CREATE TABLE `tests` (
	`id` bigint NOT NULL AUTO_INCREMENT,
	`test_curriculum_id` bigint NOT NULL,
	`training_template_id` bigint NOT NULL,
	`status` varchar(20) NULL COMMENT 'Enum: NOT_READY, NOT_STARTED, IN_PROGRESS, COMPLETED',
	`result` json NULL COMMENT '문항별 정답 여부와 오답 부분',
	`accuracy` decimal NULL COMMENT '정답률',
	`created_at` timestamp NOT NULL,
	`started_at` timestamp NULL,
	`finished_at` timestamp NULL,
	`sequence_no` int NOT NULL,
	CONSTRAINT `PK_TESTS` PRIMARY KEY (`id`)
);

CREATE TABLE `test_curriculums` (
	`id` bigint NOT NULL,
	`student_id` bigint NOT NULL,
	`status` varchar(20) NOT NULL COMMENT 'Enum: NOT_STARTED, IN_PROGRESS, COMPLETED',
	`created_at` timestamp NULL,
	`completed_at` timestamp NULL,
	CONSTRAINT `PK_TEST_CURRICULUMS` PRIMARY KEY (`id`)
);

CREATE TABLE `test_datas` (
	`id` bigint NOT NULL,
	`test_id` bigint NOT NULL,
	`generated_data` json NULL,
	`created_at` timestamp NULL,
	CONSTRAINT `PK_TEST_DATAS` PRIMARY KEY (`id`)
);

ALTER TABLE `teachers`
	ADD CONSTRAINT `UK_TEACHERS_EMAIL`
		UNIQUE (`email`);

ALTER TABLE `auth_refresh_sessions`
	ADD CONSTRAINT `UK_AUTH_REFRESH_SESSIONS_TOKEN_HASH`
		UNIQUE (`token_hash`);

ALTER TABLE `password_reset_tokens`
	ADD CONSTRAINT `UK_PASSWORD_RESET_TOKENS_TOKEN_HASH`
		UNIQUE (`token_hash`);

ALTER TABLE `words`
	ADD CONSTRAINT `UK_WORDS_CONTENT`
		UNIQUE (`content`);

ALTER TABLE `training_templates`
	ADD CONSTRAINT `UK_TRAINING_TEMPLATES_SEQUENCE`
		UNIQUE (`curriculum_unit_id`, `sequence_no`);

ALTER TABLE `story_scenes`
	ADD CONSTRAINT `UK_STORY_SCENES_SEQUENCE`
		UNIQUE (`story_id`, `sequence_no`);

ALTER TABLE `story_lines`
	ADD CONSTRAINT `UK_STORY_LINES_SEQUENCE`
		UNIQUE (`scene_id`, `sequence_no`);

ALTER TABLE `story_choices`
	ADD CONSTRAINT `UK_STORY_CHOICES_STORY_LINE`
		UNIQUE (`story_line_id`);

ALTER TABLE `trainings`
	ADD CONSTRAINT `UK_TRAININGS_SEQUENCE`
		UNIQUE (`daily_curriculum_id`, `sequence_no`);

ALTER TABLE `tests`
	ADD CONSTRAINT `UK_TESTS_SEQUENCE`
		UNIQUE (`test_curriculum_id`, `sequence_no`);

ALTER TABLE `word_categories`
	ADD CONSTRAINT `UK_WORD_CATEGORIES_WORD_CATEGORY`
		UNIQUE (`word_id`, `category_name`);

ALTER TABLE `gaze_analysis_results`
	ADD CONSTRAINT `UK_GAZE_ANALYSIS_RESULTS_SESSION`
		UNIQUE (`gaze_session_id`);

ALTER TABLE `auth_refresh_sessions`
	ADD CONSTRAINT `CHK_AUTH_REFRESH_SESSIONS_AUDIENCE`
		CHECK (
			(`audience` = 'ADMIN' AND `student_id` IS NULL)
			OR (`audience` = 'LEARNING' AND `student_id` IS NOT NULL)
		);

ALTER TABLE `gaze_sessions`
	ADD CONSTRAINT `CHK_GAZE_SESSIONS_CONTENT`
		CHECK (
			(`content_type` = 'TEST'
				AND `test_id` IS NOT NULL
				AND `training_id` IS NULL
				AND `story_id` IS NULL)
			OR (`content_type` = 'TRAINING'
				AND `test_id` IS NULL
				AND `training_id` IS NOT NULL
				AND `story_id` IS NULL)
			OR (`content_type` = 'STORY'
				AND `test_id` IS NULL
				AND `training_id` IS NULL
				AND `story_id` IS NOT NULL)
		);

ALTER TABLE `word_attempt_logs`
	ADD CONSTRAINT `CHK_WORD_ATTEMPT_LOGS_LOCATION`
		CHECK (
			(`use_location` = 'TEST'
				AND `test_id` IS NOT NULL
				AND `training_id` IS NULL
				AND `story_line_id` IS NULL)
			OR (`use_location` = 'TRAINING'
				AND `test_id` IS NULL
				AND `training_id` IS NOT NULL
				AND `story_line_id` IS NULL)
			OR (`use_location` = 'STORY'
				AND `test_id` IS NULL
				AND `training_id` IS NULL
				AND `story_line_id` IS NOT NULL)
		);

ALTER TABLE `reports`
	ADD CONSTRAINT `CHK_REPORTS_PERIOD`
		CHECK (`start_date` <= `end_date`);

ALTER TABLE `students`
	ADD CONSTRAINT `FK_STUDENTS_TEACHER`
		FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`);

ALTER TABLE `reading_features`
	ADD CONSTRAINT `FK_READING_FEATURES_PARENT`
		FOREIGN KEY (`parent_feature_id`) REFERENCES `reading_features` (`id`);

ALTER TABLE `student_feature_profiles`
	ADD CONSTRAINT `FK_STUDENT_FEATURE_PROFILES_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_STUDENT_FEATURE_PROFILES_READING_FEATURE`
		FOREIGN KEY (`reading_features_id`) REFERENCES `reading_features` (`id`);

ALTER TABLE `auth_refresh_sessions`
	ADD CONSTRAINT `FK_AUTH_REFRESH_SESSIONS_TEACHER`
		FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
	ADD CONSTRAINT `FK_AUTH_REFRESH_SESSIONS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `password_reset_tokens`
	ADD CONSTRAINT `FK_PASSWORD_RESET_TOKENS_TEACHER`
		FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`);

ALTER TABLE `training_templates`
	ADD CONSTRAINT `FK_TRAINING_TEMPLATES_CURRICULUM_UNIT`
		FOREIGN KEY (`curriculum_unit_id`) REFERENCES `curriculum_units` (`id`);

ALTER TABLE `story_scenes`
	ADD CONSTRAINT `FK_STORY_SCENES_STORY`
		FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`);

ALTER TABLE `word_categories`
	ADD CONSTRAINT `FK_WORD_CATEGORIES_WORD`
		FOREIGN KEY (`word_id`) REFERENCES `words` (`id`);

ALTER TABLE `training_datas`
	ADD CONSTRAINT `FK_TRAINING_DATAS_TRAINING`
		FOREIGN KEY (`train_id`) REFERENCES `trainings` (`id`);

ALTER TABLE `stories`
	ADD CONSTRAINT `FK_STORIES_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_STORIES_STORY_TEMPLATE`
		FOREIGN KEY (`story_template_id`) REFERENCES `story_templates` (`id`);

ALTER TABLE `gaze_analysis_results`
	ADD CONSTRAINT `FK_GAZE_ANALYSIS_RESULTS_SESSION`
		FOREIGN KEY (`gaze_session_id`) REFERENCES `gaze_sessions` (`id`);

ALTER TABLE `gaze_sessions`
	ADD CONSTRAINT `FK_GAZE_SESSIONS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_GAZE_SESSIONS_TEST`
		FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`),
	ADD CONSTRAINT `FK_GAZE_SESSIONS_TRAINING`
		FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`),
	ADD CONSTRAINT `FK_GAZE_SESSIONS_STORY`
		FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`);

ALTER TABLE `reports`
	ADD CONSTRAINT `FK_REPORTS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `story_lines`
	ADD CONSTRAINT `FK_STORY_LINES_SCENE`
		FOREIGN KEY (`scene_id`) REFERENCES `story_scenes` (`scene_id`);

ALTER TABLE `word_attempt_logs`
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_WORD`
		FOREIGN KEY (`word_id`) REFERENCES `words` (`id`),
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_STORY_LINE`
		FOREIGN KEY (`story_line_id`) REFERENCES `story_lines` (`id`),
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_TRAINING`
		FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`),
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_TEST`
		FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`);

ALTER TABLE `characters`
	ADD CONSTRAINT `FK_CHARACTER_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_CHARACTER_STORY`
		FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`);

ALTER TABLE `story_choices`
	ADD CONSTRAINT `FK_STORY_CHOICES_STORY_LINE`
		FOREIGN KEY (`story_line_id`) REFERENCES `story_lines` (`id`);

ALTER TABLE `trainings`
	ADD CONSTRAINT `FK_TRAININGS_TEMPLATE`
		FOREIGN KEY (`training_template_id`) REFERENCES `training_templates` (`id`),
	ADD CONSTRAINT `FK_TRAININGS_DAILY_CURRICULUM`
		FOREIGN KEY (`daily_curriculum_id`) REFERENCES `daily_curriculums` (`id`);

ALTER TABLE `daily_curriculums`
	ADD CONSTRAINT `FK_DAILY_CURRICULUMS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `tests`
	ADD CONSTRAINT `FK_TESTS_TEST_CURRICULUM`
		FOREIGN KEY (`test_curriculum_id`) REFERENCES `test_curriculums` (`id`),
	ADD CONSTRAINT `FK_TESTS_TRAINING_TEMPLATE`
		FOREIGN KEY (`training_template_id`) REFERENCES `training_templates` (`id`);

ALTER TABLE `test_curriculums`
	ADD CONSTRAINT `FK_TEST_CURRICULUMS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `test_datas`
	ADD CONSTRAINT `FK_TEST_DATAS_TEST`
		FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`);
