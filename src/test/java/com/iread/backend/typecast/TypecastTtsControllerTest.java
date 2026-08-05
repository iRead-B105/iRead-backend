package com.iread.backend.typecast;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypecastTtsControllerTest {

    @Test
    void usesTypecastWithTempoWhenTtsIsReal() {
        TypecastTtsClient typecast = mock(TypecastTtsClient.class);
        AiClient aiClient = mock(AiClient.class);
        byte[] audio = new byte[]{'M', 'P', '3'};
        when(typecast.synthesize("테스트", 0.8)).thenReturn(audio);
        TypecastTtsController controller = new TypecastTtsController(
                typecast, aiClient, properties(false)
        );

        var response = controller.synthesize(10L, new TypecastTtsRequest("테스트", 0.8));

        assertThat(response.getBody()).containsExactly(audio);
        verify(typecast).synthesize("테스트", 0.8);
        verify(aiClient, never()).synthesizeSpeech(any());
    }

    @Test
    void delegatesToAiClientWhenTtsIsMocked() {
        TypecastTtsClient typecast = mock(TypecastTtsClient.class);
        AiClient aiClient = mock(AiClient.class);
        byte[] mockAudio = new byte[]{'I', 'D', '3'};
        when(aiClient.synthesizeSpeech(any(SpeechSynthesisRequest.class)))
                .thenReturn(new SpeechSynthesisResponse(mockAudio, 1000));
        TypecastTtsController controller = new TypecastTtsController(
                typecast, aiClient, properties(true)
        );

        var response = controller.synthesize(10L, new TypecastTtsRequest("테스트", 0.8));

        assertThat(response.getBody()).containsExactly(mockAudio);
        ArgumentCaptor<SpeechSynthesisRequest> requestCaptor =
                ArgumentCaptor.forClass(SpeechSynthesisRequest.class);
        verify(aiClient).synthesizeSpeech(requestCaptor.capture());
        assertThat(requestCaptor.getValue().text()).isEqualTo("테스트");
        assertThat(requestCaptor.getValue().tempo()).isEqualTo(0.8);
        verify(typecast, never()).synthesize(anyString(), anyDouble());
    }

    private AiClientProperties properties(boolean mockTts) {
        return new AiClientProperties(
                URI.create("http://localhost:8081"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                "api-key",
                true,
                true,
                true,
                null,
                null,
                mockTts
        );
    }
}
