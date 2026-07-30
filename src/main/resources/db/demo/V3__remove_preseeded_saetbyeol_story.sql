-- Saetbyeol's story is created through the story session API and mock AI.
-- Keep only story templates in the demo seed; do not expose a prebuilt story.
DELETE FROM `gaze_analysis_results`
WHERE `gaze_session_id` IN (
    SELECT `id`
    FROM `gaze_sessions`
    WHERE `story_id` = 6001
);

DELETE FROM `gaze_sessions`
WHERE `story_id` = 6001;

DELETE FROM `word_attempt_logs`
WHERE `story_line_id` IN (
    SELECT `id`
    FROM `story_lines`
    WHERE `scene_id` IN (
        SELECT `scene_id`
        FROM `story_scenes`
        WHERE `story_id` = 6001
    )
);

DELETE FROM `story_choices`
WHERE `story_line_id` IN (
    SELECT `id`
    FROM `story_lines`
    WHERE `scene_id` IN (
        SELECT `scene_id`
        FROM `story_scenes`
        WHERE `story_id` = 6001
    )
);

DELETE FROM `character`
WHERE `story_id` = 6001;

DELETE FROM `story_lines`
WHERE `scene_id` IN (
    SELECT `scene_id`
    FROM `story_scenes`
    WHERE `story_id` = 6001
);

DELETE FROM `story_scenes`
WHERE `story_id` = 6001;

DELETE FROM `stories`
WHERE `id` = 6001
  AND `student_id` = 2001;
