package com.iread.backend.training.generation;

import com.iread.backend.training.domain.TrainingTemplateEntity;

import java.util.Set;

/** Controls which historical template identifiers may be used for new learning. */
public final class TrainingCatalogPolicy {

    /**
     * 6·14·24: 초기 개편 때 제거된 이력 ID.
     * 26(짧은 글 읽기)·32(끊어 읽기)·34(짧은 이야기 읽기): 읽기 훈련 통합으로
     * 25(어절별로 읽기)·33(한번에 읽기)에 흡수되어 새 편성에서 제외한다.
     * 30(문장 따라 읽기)은 모범 음성 모방이라는 다른 경험이라 유지하며,
     * 유창성 진단 트랙(템플릿 3개 필요: 30·31·33)의 최소 수량이기도 하다.
     */
    private static final Set<Long> RETIRED_TEMPLATE_IDS = Set.of(6L, 14L, 24L, 26L, 32L, 34L);

    private TrainingCatalogPolicy() {
    }

    public static boolean isSelectable(TrainingTemplateEntity template) {
        return template != null && !RETIRED_TEMPLATE_IDS.contains(template.getId());
    }

    /** [firstId, lastId] 구간에서 새 편성에 쓸 수 있는 템플릿 수. */
    public static int selectableCountInRange(long firstId, long lastId) {
        int retired = (int) RETIRED_TEMPLATE_IDS.stream()
                .filter(id -> id >= firstId && id <= lastId)
                .count();
        return Math.toIntExact(lastId - firstId + 1) - retired;
    }
}
