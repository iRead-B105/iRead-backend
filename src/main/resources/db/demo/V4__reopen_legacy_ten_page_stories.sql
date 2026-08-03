-- 과거 데모 생성기는 10페이지를 전체 이야기로 보고 progress=100/COMPLETED로 저장했다.
-- 새 10일 x 10페이지 정책에서는 이 데이터가 1일차 완료 상태이므로 다시 진행 중으로 연다.
UPDATE stories AS story
JOIN (
    SELECT scene.story_id, COUNT(line.id) AS page_count
    FROM story_scenes AS scene
    JOIN story_lines AS line ON line.scene_id = scene.scene_id
    GROUP BY scene.story_id
) AS page_totals ON page_totals.story_id = story.id
SET story.progress = page_totals.page_count,
    story.status = 'IN_PROGRESS'
WHERE story.status = 'COMPLETED'
  AND page_totals.page_count = 10;
