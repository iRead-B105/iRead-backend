package com.iread.backend.typecast;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypecastTtsControllerTest {

    @Test
    void usesAiMockTtsWhenMockTtsIsEnabled() {
        TypecastTtsClient typecastClient = mock(TypecastTtsClient.class);
        AiClient aiClient = mock(AiClient.class);
        AiClientProperties properties = mock(AiClientProperties.class);
        when(properties.ttsMocked()).thenReturn(true);
        byte[] mockAudio = new byte[]{'I', 'D', '3'};
        when(aiClient.synthesizeSpeech(any(SpeechSynthesisRequest.class)))
                .thenReturn(new SpeechSynthesisResponse(mockAudio, 1000));
        TypecastTtsController controller = new TypecastTtsController(
                typecastClient,
                aiClient,
                properties
        );

        var response = controller.synthesize(10L, new TypecastTtsRequest("테스트", 1.0));

        assertThat(response.getBody()).containsExactly(mockAudio);
        verify(typecastClient, never()).synthesize(any(), org.mockito.ArgumentMatchers.anyDouble());
    }
}
