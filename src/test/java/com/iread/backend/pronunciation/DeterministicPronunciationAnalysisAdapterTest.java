package com.iread.backend.pronunciation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicPronunciationAnalysisAdapterTest {

    private final DeterministicPronunciationAnalysisAdapter adapter =
            new DeterministicPronunciationAnalysisAdapter();

    @Test
    void returnsDeterministicSuccessWithoutPersistingAudio() {
        PronunciationAnalysisResult result = adapter.analyze(new PronunciationAnalysisRequest(
                "request-1",
                "먹는다",
                "attempt.wav",
                new byte[]{1, 2, 3}
        ));

        assertThat(result.pronunciationAccuracyScore()).isEqualTo(95.0);
        assertThat(result.confidence()).isEqualTo(0.96);
        assertThat(result.errorType()).isEqualTo("NONE");
        assertThat(result.analysisVersion()).isEqualTo("PRONUNCIATION_MOCK_V1");
    }

    @Test
    void errorFixtureReturnsGenericPronunciationMismatch() {
        PronunciationAnalysisResult result = adapter.analyze(new PronunciationAnalysisRequest(
                "request-2",
                "먹는다",
                "nasalization-error.wav",
                new byte[]{4, 5, 6}
        ));

        assertThat(result.pronunciationAccuracyScore()).isEqualTo(54.2);
        assertThat(result.errorType()).isEqualTo("PRONUNCIATION_MISMATCH");
    }
}
