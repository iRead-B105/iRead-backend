package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockSpeechProcessorTest {

    private final MockSpeechProcessor processor = new MockSpeechProcessor();

    @Test
    void returnsExpectedTextForReadingAndStableBranchFixture() {
        assertThat(processor.transcribe("request-1", "책을 읽어요").transcript())
                .isEqualTo("책을 읽어요");
        assertThat(processor.transcribe("request-2", null).transcript())
                .isEqualTo("친구를 따라간다");
    }

    @Test
    void returnsDeterministicTtsBytesAndDuration() {
        SpeechSynthesisRequest request = new SpeechSynthesisRequest(
                "request-1", "책을 읽어요", null, 1.0
        );

        var first = processor.synthesize(request);
        var second = processor.synthesize(request);

        assertThat(first.audio()).isEqualTo(second.audio());
        assertThat(first.durationMs()).isEqualTo(second.durationMs());
        assertThat(first.audio()).startsWith((byte) 'I', (byte) 'D', (byte) '3');
    }

    @Test
    void adjustsDeterministicDurationByTempo() {
        var slow = processor.synthesize(new SpeechSynthesisRequest(
                "slow", "천천히 책을 읽어요", null, 0.5
        ));
        var fast = processor.synthesize(new SpeechSynthesisRequest(
                "fast", "천천히 책을 읽어요", null, 2.0
        ));

        assertThat(slow.durationMs()).isGreaterThan(fast.durationMs());
    }
}
