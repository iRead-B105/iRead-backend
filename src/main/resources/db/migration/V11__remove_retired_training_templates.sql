CREATE TEMPORARY TABLE retired_training_ids (
    id BIGINT PRIMARY KEY
);

INSERT INTO retired_training_ids (id)
SELECT id
FROM trainings
WHERE training_template_id IN (6, 14, 24);

CREATE TEMPORARY TABLE retired_test_ids (
    id BIGINT PRIMARY KEY
);

INSERT INTO retired_test_ids (id)
SELECT id
FROM tests
WHERE training_template_id IN (6, 14, 24);

DELETE analysis
FROM gaze_analysis_results analysis
JOIN gaze_sessions session ON session.id = analysis.gaze_session_id
LEFT JOIN retired_training_ids training ON training.id = session.training_id
LEFT JOIN retired_test_ids test ON test.id = session.test_id
WHERE training.id IS NOT NULL OR test.id IS NOT NULL;

DELETE attempt
FROM word_attempt_logs attempt
LEFT JOIN retired_training_ids training ON training.id = attempt.training_id
LEFT JOIN retired_test_ids test ON test.id = attempt.test_id
WHERE training.id IS NOT NULL OR test.id IS NOT NULL;

DELETE session
FROM gaze_sessions session
LEFT JOIN retired_training_ids training ON training.id = session.training_id
LEFT JOIN retired_test_ids test ON test.id = session.test_id
WHERE training.id IS NOT NULL OR test.id IS NOT NULL;

DELETE data
FROM training_datas data
JOIN retired_training_ids training ON training.id = data.train_id;

DELETE data
FROM test_datas data
JOIN retired_test_ids test ON test.id = data.test_id;

DELETE training
FROM trainings training
JOIN retired_training_ids retired ON retired.id = training.id;

DELETE test
FROM tests test
JOIN retired_test_ids retired ON retired.id = test.id;

DELETE FROM training_templates
WHERE id IN (6, 14, 24);

DROP TEMPORARY TABLE retired_test_ids;
DROP TEMPORARY TABLE retired_training_ids;
