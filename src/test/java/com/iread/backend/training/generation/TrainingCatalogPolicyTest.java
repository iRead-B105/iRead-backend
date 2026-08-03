package com.iread.backend.training.generation;

import com.iread.backend.training.domain.TrainingTemplateEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingCatalogPolicyTest {

    @Test
    void excludesRetiredClassificationAndPhonemeBlendTemplatesFromNewLearning() {
        TrainingTemplateEntity retiredClassification = mock(TrainingTemplateEntity.class);
        when(retiredClassification.getId()).thenReturn(6L);
        TrainingTemplateEntity retiredPhonemeBlend = mock(TrainingTemplateEntity.class);
        when(retiredPhonemeBlend.getId()).thenReturn(14L);
        TrainingTemplateEntity selectable = mock(TrainingTemplateEntity.class);
        when(selectable.getId()).thenReturn(15L);

        assertThat(TrainingCatalogPolicy.isSelectable(retiredClassification)).isFalse();
        assertThat(TrainingCatalogPolicy.isSelectable(retiredPhonemeBlend)).isFalse();
        assertThat(TrainingCatalogPolicy.isSelectable(selectable)).isTrue();
    }
}
