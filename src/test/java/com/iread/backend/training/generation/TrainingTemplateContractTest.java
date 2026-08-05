package com.iread.backend.training.generation;

import com.iread.backend.training.domain.TrainingTemplateEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingTemplateContractTest {

    @Test
    void resolvesStableTrainingTypeWithoutDependingOnTemplateId() {
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn(9999L);
        when(template.getPrompt()).thenReturn(
                "{\"trainingType\":\"SYLLABLE_TRACE\"}"
        );

        assertThat(TrainingTemplateContract.trainingType(
                template,
                JsonMapper.builder().build()
        )).isEqualTo(TrainingType.SYLLABLE_TRACE);
    }

    @Test
    void rejectsTemplateWithoutSupportedTrainingType() {
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn(9999L);
        when(template.getPrompt()).thenReturn(
                "{\"trainingType\":\"UNKNOWN_TYPE\"}"
        );

        assertThatThrownBy(() -> TrainingTemplateContract.trainingType(
                template,
                JsonMapper.builder().build()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("훈련 템플릿의 trainingType을 읽을 수 없습니다: 9999");
    }
}
