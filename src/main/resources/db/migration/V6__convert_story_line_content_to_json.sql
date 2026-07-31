-- 스토리 대사 content를 대사 본문 + 형태소·G2P 분석 결과를 함께 담는 JSON으로 전환한다.
-- 이미 저장된 평문 대사는 {"text": 기존값} 으로 감싸고, analysis는 처음 읽힐 때 채운다.

UPDATE `story_lines`
   SET `content` = JSON_OBJECT('text', `content`)
 WHERE JSON_VALID(`content`) = 0;

UPDATE `story_lines`
   SET `content` = JSON_OBJECT('text', `content`)
 WHERE JSON_VALID(`content`) = 1
   AND JSON_TYPE(`content`) <> 'OBJECT';

ALTER TABLE `story_lines`
	MODIFY COLUMN `content` json NOT NULL
		COMMENT '대사 본문과 형태소·G2P 분석 결과';
