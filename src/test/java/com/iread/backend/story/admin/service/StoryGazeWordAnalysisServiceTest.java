package com.iread.backend.story.admin.service;

import com.iread.backend.story.admin.dto.res.StoryGazeAnalysisResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryGazeWordAnalysisServiceTest {

    private final StoryGazeWordAnalysisService service = new StoryGazeWordAnalysisService();
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void classifiesSkippedIntermediateTokenAndPreservesEventAfterLaterRead() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"storyLineId":50,"tokenIndex":0,"capturedAtMs":0},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":0,"capturedAtMs":100},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":0,"capturedAtMs":200},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":2,"capturedAtMs":300},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":2,"capturedAtMs":400},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":2,"capturedAtMs":500},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":2,"capturedAtMs":600},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":1,"capturedAtMs":700},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":1,"capturedAtMs":800},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":1,"capturedAtMs":900}
                ]}
                """);

        var result = service.analyze(
                List.of(new StoryGazeWordAnalysisService.Page(50L, 1, "가 나 다 라")),
                List.of(payload)
        );

        assertThat(result.events()).extracting(StoryGazeAnalysisResponse.ReplayEvent::movementType)
                .containsExactly(
                        StoryGazeAnalysisResponse.MovementType.READ,
                        StoryGazeAnalysisResponse.MovementType.SKIP,
                        StoryGazeAnalysisResponse.MovementType.REGRESSION
                );
        assertThat(result.events().get(1).skippedTokenIndexes()).containsExactly(1);
        assertThat(result.wordMetrics().get(1).skipped()).isFalse();
        assertThat(result.wordMetrics().get(1).regressionCount()).isEqualTo(1);
        assertThat(result.wordMetrics().get(1).firstSeenMs()).isEqualTo(700);
    }

    @Test
    void splitsVisitsForDifferentTokenOffWordAndGapOver250Ms() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":0},
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":100},
                  {"pageNo":1,"capturedAtMs":150},
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":200},
                  {"pageNo":1,"tokenIndex":1,"capturedAtMs":300},
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":400},
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":700}
                ]}
                """);

        var result = service.analyze(
                List.of(new StoryGazeWordAnalysisService.Page(50L, 1, "가 나")),
                List.of(payload)
        );

        assertThat(result.wordMetrics().getFirst().visitCount()).isEqualTo(4);
        assertThat(result.events()).extracting(StoryGazeAnalysisResponse.ReplayEvent::toTokenIndex)
                .containsExactly(0, 0, 1, 0, 0);
    }

    @Test
    void appliesStrictGreaterThanDwellBoundaryAnd80MsTail() throws Exception {
        JsonNode equalBoundary = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":0},
                  {"pageNo":1,"tokenIndex":1,"capturedAtMs":80}
                ]}
                """);
        JsonNode greaterBoundary = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":0},
                  {"pageNo":1,"tokenIndex":1,"capturedAtMs":80},
                  {"pageNo":1,"tokenIndex":1,"capturedAtMs":160}
                ]}
                """);
        var page = new StoryGazeWordAnalysisService.Page(50L, 1, "가 나");

        var equalResult = service.analyze(List.of(page), List.of(equalBoundary));
        var greaterResult = service.analyze(List.of(page), List.of(greaterBoundary));

        assertThat(equalResult.events()).allMatch(event -> !event.dwellQualified());
        assertThat(equalResult.events()).extracting(StoryGazeAnalysisResponse.ReplayEvent::dwellDurationMs)
                .containsExactly(80, 80);
        assertThat(greaterResult.events().get(1).dwellQualified()).isTrue();
        assertThat(greaterResult.events().get(1).dwellDurationMs()).isEqualTo(160);
    }

    @Test
    void distinguishesRepeatedTextByTokenIndexAndIgnoresInvalidPageIdentity() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"storyLineId":50,"tokenIndex":0,"text":"가","capturedAtMs":0},
                  {"pageNo":1,"storyLineId":50,"tokenIndex":2,"text":"가","capturedAtMs":100},
                  {"pageNo":2,"storyLineId":50,"tokenIndex":1,"capturedAtMs":200}
                ]}
                """);

        var result = service.analyze(
                List.of(new StoryGazeWordAnalysisService.Page(50L, 1, "가 나 가")),
                List.of(payload)
        );

        assertThat(result.wordMetrics()).extracting(StoryGazeAnalysisResponse.WordMetric::tokenIndex)
                .containsExactly(0, 1, 2);
        assertThat(result.wordMetrics().get(0).visitCount()).isEqualTo(1);
        assertThat(result.wordMetrics().get(2).visitCount()).isEqualTo(1);
        assertThat(result.events()).hasSize(2);
    }

    @Test
    void returnsNoEstimatesWithoutValidRawSamples() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"replayWords":[{"pageNo":1,"tokenIndex":0,"dwellMs":1000}]}
                """);

        var result = service.analyze(
                List.of(new StoryGazeWordAnalysisService.Page(50L, 1, "가 나")),
                List.of(payload)
        );

        assertThat(result.wordMetrics()).isEmpty();
        assertThat(result.events()).isEmpty();
    }

    @Test
    void usesNewestCompletedPayloadPerPageAndKeepsOlderDifferentPage() throws Exception {
        JsonNode newest = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":1000},
                  {"pageNo":1,"tokenIndex":0,"capturedAtMs":1100}
                ]}
                """);
        JsonNode older = objectMapper.readTree("""
                {"samples":[
                  {"pageNo":1,"tokenIndex":1,"capturedAtMs":0},
                  {"pageNo":2,"tokenIndex":1,"capturedAtMs":0}
                ]}
                """);

        var result = service.analyze(
                List.of(
                        new StoryGazeWordAnalysisService.Page(50L, 1, "가 나"),
                        new StoryGazeWordAnalysisService.Page(51L, 2, "다 라")
                ),
                List.of(newest, older)
        );

        assertThat(result.wordMetrics())
                .filteredOn(metric -> metric.pageNo() == 1 && metric.tokenIndex() == 0)
                .singleElement()
                .extracting(StoryGazeAnalysisResponse.WordMetric::visitCount)
                .isEqualTo(1);
        assertThat(result.wordMetrics())
                .filteredOn(metric -> metric.pageNo() == 1 && metric.tokenIndex() == 1)
                .singleElement()
                .extracting(StoryGazeAnalysisResponse.WordMetric::visitCount)
                .isEqualTo(0);
        assertThat(result.wordMetrics())
                .filteredOn(metric -> metric.pageNo() == 2 && metric.tokenIndex() == 1)
                .singleElement()
                .extracting(StoryGazeAnalysisResponse.WordMetric::visitCount)
                .isEqualTo(1);
        assertThat(result.events())
                .extracting(StoryGazeAnalysisResponse.ReplayEvent::eventIndex)
                .containsExactly(0, 1);
    }
}
