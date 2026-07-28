package com.iread.backend.gaze.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicGazeWordAnalysisAdapterTest {

    private final DeterministicGazeWordAnalysisAdapter adapter =
            new DeterministicGazeWordAnalysisAdapter();

    @Test
    void sameInputProducesSameWordMetricsWithoutRawCoordinates() {
        GazeWordAnalysisRequest request = new GazeWordAnalysisRequest(
                "gaze-120-1-0", "먹는다", 100, 1_700
        );

        GazeWordAnalysisResult first = adapter.analyze(request);
        GazeWordAnalysisResult second = adapter.analyze(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.fixationDurationMs()).isEqualTo(1_600);
        assertThat(first.fixationCount()).isEqualTo(4);
        assertThat(first.regressionCount()).isEqualTo(2);
        assertThat(first.skipped()).isFalse();
        assertThat(first.analysisVersion()).isEqualTo("GAZE_MOCK_V1");
    }
}
