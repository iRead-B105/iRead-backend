package com.iread.backend.typecast;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypecastTtsControllerTest {

    @Test
    void delegatesTtsToAiServer() {
        AiClient aiClient = mock(AiClient.class);
        byte[] mockAudio = new byte[]{'I', 'D', '3'};
        when(aiClient.synthesizeSpeech(any(SpeechSynthesisRequest.class)))
                .thenReturn(new SpeechSynthesisResponse(mockAudio, 1000));
        TypecastTtsController controller = new TypecastTtsController(aiClient);

        var response = controller.synthesize(10L, new TypecastTtsRequest("테스트", 0.8));

        assertThat(response.getBody()).containsExactly(mockAudio);
        ArgumentCaptor<SpeechSynthesisRequest> requestCaptor =
                ArgumentCaptor.forClass(SpeechSynthesisRequest.class);
        verify(aiClient).synthesizeSpeech(requestCaptor.capture());
        assertThat(requestCaptor.getValue().text()).isEqualTo("테스트");
        assertThat(requestCaptor.getValue().tempo()).isEqualTo(0.8);
    }
}
