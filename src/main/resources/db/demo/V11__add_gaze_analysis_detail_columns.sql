ALTER TABLE `gaze_analysis_results`
    ADD COLUMN `sentence_metrics` json NULL AFTER `avg_visited_duration`,
    ADD COLUMN `regressions` json NULL AFTER `sentence_metrics`,
    ADD COLUMN `analysis_meta` json NULL AFTER `regressions`;
