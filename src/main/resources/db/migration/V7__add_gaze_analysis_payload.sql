ALTER TABLE `gaze_analysis_results`
    ADD COLUMN `sentence_metrics` JSON NULL
        COMMENT '문장 또는 스토리 라인 단위 시선 분석 결과',
    ADD COLUMN `regressions` JSON NULL
        COMMENT '역행 이벤트 목록',
    ADD COLUMN `analysis_meta` JSON NULL
        COMMENT '분석 버전과 원본 콘텐츠 식별 정보';
