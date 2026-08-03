package com.iread.backend.training.generation;

import com.iread.backend.training.domain.TrainingTemplateEntity;

import java.util.Set;

/** Controls which historical template identifiers may be used for new learning. */
public final class TrainingCatalogPolicy {

    private static final Set<Long> RETIRED_TEMPLATE_IDS = Set.of(6L, 14L, 24L);

    private TrainingCatalogPolicy() {
    }

    public static boolean isSelectable(TrainingTemplateEntity template) {
        return template != null && !RETIRED_TEMPLATE_IDS.contains(template.getId());
    }
}
