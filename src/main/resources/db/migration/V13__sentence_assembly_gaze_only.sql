-- 문장 전체 조립(SENTENCE_ASSEMBLY, 템플릿 27)은 녹음을 요구하지 않는 훈련이다.
-- 정책(TrainingInputPolicy)이 GAZE 단독으로 바뀌면서, 이미 VOICE가 박힌
-- 템플릿 prompt와 생성된 훈련 문항(generated_data)을 GAZE 단독으로 맞춘다.
-- prompt는 TEXT(잭슨 압축 표기), generated_data는 JSON(MySQL 정규화 표기)이라
-- 두 표기를 모두 치환한다.

UPDATE training_templates
SET prompt = REPLACE(
        REPLACE(
            prompt,
            '"requiredInputs":["VOICE","GAZE"]',
            '"requiredInputs":["GAZE"]'
        ),
        '"requiredInputs": ["VOICE", "GAZE"]',
        '"requiredInputs": ["GAZE"]'
    )
WHERE id = 27;

-- 템플릿 27로 생성된 훈련은 모든 문항이 SENTENCE_ASSEMBLY이므로
-- 행 안의 requiredInputs를 일괄 치환해도 다른 유형을 건드리지 않는다.
UPDATE training_datas d
JOIN trainings t ON t.id = d.train_id
SET d.generated_data = REPLACE(
        REPLACE(
            CAST(d.generated_data AS CHAR),
            '"requiredInputs": ["VOICE", "GAZE"]',
            '"requiredInputs": ["GAZE"]'
        ),
        '"requiredInputs":["VOICE","GAZE"]',
        '"requiredInputs":["GAZE"]'
    )
WHERE t.training_template_id = 27;
