CREATE TABLE `training_templates` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`curriculum_unit_id`	bigint	NOT NULL,
	`name`	varchar(100)	NOT NULL,
	`form`	json	NOT NULL,
	`sequence_no`	int	NOT NULL
);

CREATE TABLE `students` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`teacher_id`	bigint	NOT NULL	COMMENT 'foreign key',
	`name`	varchar(10)	NOT NULL	COMMENT '실명',
	`birthday`	date	NULL,
	`gender`	varchar(10)	NULL	COMMENT 'Enum: boy, girl',
	`school`	varchar(20)	NULL,
	`guardian`	varchar(100)	NULL,
	`guardian_contact`	varchar(20)	NULL,
	`guardian_email`	varchar(50)	NULL,
	`address`	varchar(100)	NULL,
	`created_at`	timestamp	NOT NULL	COMMENT '생성일',
	`image_url`	varchar(255)	NULL,
	`teacher_memo`	text	NULL
);

CREATE TABLE `curriculum_units` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`unit_name`	varchar(50)	NOT NULL	COMMENT '파닉스, 단어 etc',
	`sequence_no`	int	NOT NULL
);

CREATE TABLE `sounds` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`question_number`	int	NOT NULL,
	`original_file_name`	varchar(255)	NOT NULL,
	`file_size`	bigint	NOT NULL,
	`created_at`	timestamp	NOT NULL,
	`store_file_name`	varchar(255)	NOT NULL,
	`url`	varchar(255)	NOT NULL
);

CREATE TABLE `story_templates` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`title`	varchar(50)	NOT NULL,
	`content`	text	NOT NULL	COMMENT 'ai에게 컨텍스트주는용도(ex: 백설공주의 줄거리)'
);

CREATE TABLE `images` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`original_file_name`	varchar(255)	NOT NULL,
	`store_file_name`	varchar(255)	NOT NULL,
	`file_size`	bigint	NOT NULL,
	`url`	varchar(255)	NOT NULL,
	`created_at`	timestamp	NOT NULL
);

CREATE TABLE `word_categories` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`word_id`	bigint	NOT NULL,
	`category_name`	varchar(50)	NOT NULL	COMMENT 'Enum: 받침없는단어, ㅆ받침단어 etc'
);

CREATE TABLE `training_contents` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`training_id`	bigint	NOT NULL,
	`generated_data`	json	NULL	COMMENT '훈련에서 쓰일 단어/문장(ai생성)',
	`created_at`	timestamp	NULL
);

CREATE TABLE `stories` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`story_template_id`	bigint	NOT NULL,
	`created_at`	timestamp	NOT NULL,
	`status`	varchar(30)	NOT NULL	COMMENT 'Enum: IN_PROGRESS/COMPLETED/DELETED',
	`progress`	tinyint unsigned	NOT NULL	DEFAULT 0	COMMENT 'AI 이야기 생성 요청과 응답에 사용하는 현재 이야기 진행률(0~100)',
	CONSTRAINT `CHK_STORIES_PROGRESS` CHECK (`progress` BETWEEN 0 AND 100)
);

CREATE TABLE `test_questions` (
	`id`	VARCHAR(255)	NOT NULL	PRIMARY KEY,
	`test_id`	bigint	NOT NULL,
	`question`	json	NULL	COMMENT '문항문제'
);

CREATE TABLE `gaze_analysis_results` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`gaze_session_id`	bigint	NOT NULL,
	`total_visited_duration`	int	NOT NULL,
	`total_visited_count`	int	NOT NULL,
	`reverse_read_count`	int	NOT NULL,
	`avg_visited_duration`	int	NULL,
	`created_at`	timestamp	NOT NULL
);

CREATE TABLE `gaze_sessions` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`test_id`	bigint	NULL,
	`training_id`	bigint	NULL,
	`story_id`	bigint	NULL,
	`content_type`	varchar(20)	NOT NULL	COMMENT 'TEST, TRAINING, STORY',
	`started_at`	timestamp	NOT NULL,
	`ended_at`	timestamp	NULL,
	`status`	varchar(20)	NOT NULL	COMMENT 'READY, RUNNING, COMPLETED, FAILED',
	`calibration_status`	varchar(20)	NOT NULL	COMMENT 'NOT_STARTED, SUCCESS, FAILED, SKIPPED',
	`created_at`	timestamp	NOT NULL
);

CREATE TABLE `student_study_progresses` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`training_template_id`	bigint	NOT NULL,
	`achievement`	tinyint unsigned	NOT NULL	DEFAULT 0,
	CONSTRAINT `CHK_STUDENT_STUDY_PROGRESS_ACHIEVEMENT`
		CHECK (`achievement` BETWEEN 0 AND 100)
);

CREATE TABLE `student_word_stats` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY	COMMENT '훈련+테스트+스토리 통계테이블',
	`student_id`	bigint	NOT NULL,
	`word_id`	bigint	NOT NULL,
	`word_score`	decimal(5,2)	NOT NULL	DEFAULT 0.00	COMMENT '0.0~100.0',
	`correct_count`	int unsigned	NOT NULL	DEFAULT 0,
	`failed_count`	int unsigned	NOT NULL	DEFAULT 0,
	`attempt_count`	int unsigned	NOT NULL	DEFAULT 0,
	`updated_at`	timestamp	NULL,
	CONSTRAINT `CHK_STUDENT_WORD_STATS_SCORE`
		CHECK (`word_score` BETWEEN 0.00 AND 100.00)
);

CREATE TABLE `words` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY	COMMENT '단어는 기본형태',
	`content`	varchar(50)	NOT NULL	COMMENT 'UNIQUE',
	`length`	int	NOT NULL
);

CREATE TABLE `reports` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`start_date`	date	NOT NULL	COMMENT '기간시작일~기간종료일 에 대한 리포트',
	`end_date`	date	NOT NULL,
	`snapshot_data`	json	NOT NULL,
	`teacher_memo`	text	NULL,
	`created_at`	timestamp	NOT NULL	COMMENT '생성일'
);

CREATE TABLE `videos` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`question_number`	int	NOT NULL,
	`original_file_name`	varchar(255)	NOT NULL,
	`file_size`	bigint	NOT NULL,
	`created_at`	timestamp	NOT NULL,
	`store_file_name`	varchar(255)	NOT NULL,
	`url`	varchar(255)	NOT NULL
);

CREATE TABLE `teachers` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`email`	varchar(50)	NOT NULL	COMMENT 'unique',
	`password`	varchar(100)	NOT NULL,
	`name`	varchar(10)	NOT NULL	COMMENT '실명',
	`organization`	varchar(100)	NULL,
	`created_at`	timestamp	NOT NULL	COMMENT '생성일',
	`gender`	varchar(10)	NULL	COMMENT 'Enum',
	`image_url`	varchar(255)	NULL
);

CREATE TABLE `story_lines` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`story_id`	bigint	NOT NULL,
	`previous_line_id`	bigint	NULL,
	`image_url`	varchar(255)	NULL,
	`requires_branch_input`	boolean	NOT NULL	COMMENT '아동의 음성 입력을 받아 AI로 다음 이야기 분기를 생성해야 하는 장면 여부',
	`content`	text	NOT NULL,
	`sequence_no`	int	NOT NULL,
	`created_at`	timestamp	NOT NULL,
	`read_at`	timestamp	NULL
);

CREATE TABLE `word_attempt_logs` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`word_id`	bigint	NOT NULL	COMMENT '단어는 기본형태',
	`story_line_id`	bigint	NULL,
	`training_id`	bigint	NULL,
	`test_id`	bigint	NULL,
	`use_location`	varchar(10)	NOT NULL	COMMENT 'Enum: TEST, TRAINING, STORY',
	`surface_text`	varchar(50)	NULL	COMMENT '단어의 문장내에서의 형태(ex: 먹었다)',
	`has_gaze_data`	boolean	NOT NULL,
	`has_audio_data`	boolean	NOT NULL,
	`fixation_duration_ms`	int	NULL,
	`fixation_count`	int	NULL,
	`gaze_start_offset_ms`	int	NULL,
	`gaze_end_offset_ms`	int	NULL,
	`is_skipped`	boolean	NULL,
	`regression_count`	int	NULL,
	`recognized_text`	varchar(255)	NULL	COMMENT 'stt가 인식한 결과',
	`speech_start_offset_ms`	int	NULL,
	`speech_end_offset_ms`	int	NULL,
	`is_correct`	boolean	NULL,
	`total_score`	int unsigned	NOT NULL,
	`created_at`	timestamp	NOT NULL
);

CREATE TABLE `character` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`image_url`	varchar(255)	NULL,
	`is_representative`	boolean	NOT NULL	DEFAULT false,
	`created_at`	timestamp	NOT NULL
);

CREATE TABLE `trainings` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`training_template_id`	bigint	NOT NULL,
	`daily_curriculum_id`	bigint	NOT NULL,
	`sequence_no`	int	NOT NULL	COMMENT '커리큘럼내에서의 순서, (id, 훈련순서) uniuque',
	`created_at`	timestamp	NOT NULL,
	`started_at`	timestamp	NULL,
	`finished_at`	timestamp	NULL,
	`status`	varchar(20)	NOT NULL	COMMENT 'Enum:    NOT_READY,  NOT_STARTED,  IN_PROGRESS,  COMPLETED',
	`result`	json	NULL	COMMENT '문항별 정답유무/틀린부분',
	`accuracy`	decimal(5,2)	NULL	COMMENT '정답률',
	CONSTRAINT `CHK_TRAININGS_ACCURACY`
		CHECK (`accuracy` BETWEEN 0.00 AND 100.00)
);

CREATE TABLE `daily_curriculums` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`status`	varchar(20)	NOT NULL	COMMENT 'Enum:  NOT_STARTED,  IN_PROGRESS,  COMPLETED',
	`created_at`	timestamp	NOT NULL,
	`completed_at`	timestamp	NULL
);

CREATE TABLE `tests` (
	`id`	bigint	NOT NULL	AUTO_INCREMENT	PRIMARY KEY,
	`student_id`	bigint	NOT NULL,
	`created_at`	timestamp	NOT NULL,
	`status`	varchar(20)	NULL	COMMENT 'Enum:    NOT_READY,  NOT_STARTED,  IN_PROGRESS,  COMPLETED',
	`result`	json	NULL	COMMENT '문항별 정답유무/틀린부분',
	`accuracy`	decimal(5,2)	NULL	COMMENT '정답률',
	CONSTRAINT `CHK_TEST_ACCURACY`
		CHECK (`accuracy` BETWEEN 0.00 AND 100.00)
);

ALTER TABLE `teachers`
	ADD CONSTRAINT `UK_TEACHERS_EMAIL` UNIQUE (`email`);

ALTER TABLE `words`
	ADD CONSTRAINT `UK_WORDS_CONTENT` UNIQUE (`content`);

ALTER TABLE `training_templates`
	ADD CONSTRAINT `UK_TRAINING_TEMPLATES_SEQUENCE`
		UNIQUE (`curriculum_unit_id`, `sequence_no`);

ALTER TABLE `word_categories`
	ADD CONSTRAINT `UK_WORD_CATEGORIES_WORD_CATEGORY`
		UNIQUE (`word_id`, `category_name`);

ALTER TABLE `student_study_progresses`
	ADD CONSTRAINT `UK_STUDENT_STUDY_PROGRESS`
		UNIQUE (`student_id`, `training_template_id`);

ALTER TABLE `student_word_stats`
	ADD CONSTRAINT `UK_STUDENT_WORD_STATS`
		UNIQUE (`student_id`, `word_id`);

ALTER TABLE `story_lines`
	ADD CONSTRAINT `UK_STORY_LINES_SEQUENCE`
		UNIQUE (`story_id`, `sequence_no`);

ALTER TABLE `trainings`
	ADD CONSTRAINT `UK_TRAININGS_SEQUENCE`
		UNIQUE (`daily_curriculum_id`, `sequence_no`);

ALTER TABLE `reports`
	ADD CONSTRAINT `CHK_REPORTS_PERIOD`
		CHECK (`start_date` <= `end_date`);

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

ALTER TABLE `students`
	ADD CONSTRAINT `FK_STUDENTS_TEACHER`
		FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`);

ALTER TABLE `training_templates`
	ADD CONSTRAINT `FK_TRAINING_TEMPLATES_CURRICULUM_UNIT`
		FOREIGN KEY (`curriculum_unit_id`) REFERENCES `curriculum_units` (`id`);

ALTER TABLE `word_categories`
	ADD CONSTRAINT `FK_WORD_CATEGORIES_WORD`
		FOREIGN KEY (`word_id`) REFERENCES `words` (`id`);

ALTER TABLE `training_contents`
	ADD CONSTRAINT `FK_TRAINING_CONTENTS_TRAINING`
		FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`);

ALTER TABLE `stories`
	ADD CONSTRAINT `FK_STORIES_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_STORIES_STORY_TEMPLATE`
		FOREIGN KEY (`story_template_id`) REFERENCES `story_templates` (`id`);

ALTER TABLE `test_questions`
	ADD CONSTRAINT `FK_TEST_QUESTIONS_TEST`
		FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`);

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

ALTER TABLE `student_study_progresses`
	ADD CONSTRAINT `FK_STUDENT_STUDY_PROGRESS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_STUDENT_STUDY_PROGRESS_TEMPLATE`
		FOREIGN KEY (`training_template_id`) REFERENCES `training_templates` (`id`);

ALTER TABLE `student_word_stats`
	ADD CONSTRAINT `FK_STUDENT_WORD_STATS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_STUDENT_WORD_STATS_WORD`
		FOREIGN KEY (`word_id`) REFERENCES `words` (`id`);

ALTER TABLE `reports`
	ADD CONSTRAINT `FK_REPORTS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `story_lines`
	ADD CONSTRAINT `FK_STORY_LINES_STORY`
		FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`);

ALTER TABLE `word_attempt_logs`
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
	ADD CONSTRAINT `FK_WORD_ATTEMPT_LOGS_WORD`
		FOREIGN KEY (`word_id`) REFERENCES `words` (`id`);

ALTER TABLE `character`
	ADD CONSTRAINT `FK_CHARACTER_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `trainings`
	ADD CONSTRAINT `FK_TRAININGS_TEMPLATE`
		FOREIGN KEY (`training_template_id`) REFERENCES `training_templates` (`id`),
	ADD CONSTRAINT `FK_TRAININGS_DAILY_CURRICULUM`
		FOREIGN KEY (`daily_curriculum_id`) REFERENCES `daily_curriculums` (`id`);

ALTER TABLE `daily_curriculums`
	ADD CONSTRAINT `FK_DAILY_CURRICULUMS_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

ALTER TABLE `tests`
	ADD CONSTRAINT `FK_TEST_STUDENT`
		FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);
