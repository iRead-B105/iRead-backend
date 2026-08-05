package com.iread.backend.story.admin.service;

import com.iread.backend.story.admin.dto.res.StoryGazeAnalysisResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoryGazeWordAnalysisService {

    static final int SAMPLE_TAIL_MS = 80;
    static final int MAX_SAMPLE_GAP_MS = 250;

    public Analysis analyze(List<Page> pages, List<JsonNode> storedPayloads) {
        if (pages.isEmpty() || storedPayloads.isEmpty()) {
            return Analysis.empty();
        }

        Map<Integer, PageContext> pagesByNo = new LinkedHashMap<>();
        Map<Long, PageContext> pagesByLineId = new HashMap<>();
        for (Page page : pages) {
            PageContext context = new PageContext(page, tokens(page.surfaceText()));
            pagesByNo.put(page.pageNo(), context);
            pagesByLineId.put(page.storyLineId(), context);
        }

        Map<Integer, List<Sample>> samplesByPage = new HashMap<>();
        int ordinal = 0;
        for (JsonNode storedPayload : storedPayloads) {
            JsonNode data = unwrapRawData(storedPayload);
            JsonNode sourceSamples = data == null ? null : data.path("samples");
            if (sourceSamples == null || !sourceSamples.isArray()) {
                continue;
            }
            Map<Integer, List<Sample>> payloadSamplesByPage = new HashMap<>();
            for (JsonNode source : sourceSamples) {
                Sample sample = toSample(source, pagesByNo, pagesByLineId, ordinal++);
                if (sample == null) {
                    continue;
                }
                payloadSamplesByPage.computeIfAbsent(sample.pageNo(), ignored -> new ArrayList<>())
                        .add(sample);
            }
            for (Map.Entry<Integer, List<Sample>> entry : payloadSamplesByPage.entrySet()) {
                boolean hasValidTokenSample = entry.getValue().stream()
                        .anyMatch(sample -> sample.tokenIndex() != null);
                if (hasValidTokenSample) {
                    samplesByPage.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }

        List<StoryGazeAnalysisResponse.WordMetric> wordMetrics = new ArrayList<>();
        List<StoryGazeAnalysisResponse.ReplayEvent> events = new ArrayList<>();
        for (PageContext page : pagesByNo.values()) {
            List<Sample> pageSamples = samplesByPage.getOrDefault(page.page().pageNo(), List.of())
                    .stream()
                    .sorted(Comparator.comparingLong(Sample::capturedAtMs)
                            .thenComparingInt(Sample::ordinal))
                    .toList();
            PageAnalysis analysis = analyzePage(page, pageSamples);
            wordMetrics.addAll(analysis.wordMetrics());
            for (StoryGazeAnalysisResponse.ReplayEvent event : analysis.events()) {
                events.add(new StoryGazeAnalysisResponse.ReplayEvent(
                        event.pageNo(),
                        events.size(),
                        event.eventAtMs(),
                        event.fromTokenIndex(),
                        event.toTokenIndex(),
                        event.movementType(),
                        event.dwellQualified(),
                        event.dwellDurationMs(),
                        event.skippedTokenIndexes()
                ));
            }
        }
        return new Analysis(wordMetrics, events);
    }

    private PageAnalysis analyzePage(PageContext page, List<Sample> samples) {
        List<Sample> validSamples = samples.stream()
                .filter(sample -> sample.tokenIndex() != null)
                .toList();
        if (validSamples.isEmpty() || page.tokens().isEmpty()) {
            return PageAnalysis.empty();
        }

        long pageStartMs = validSamples.getFirst().capturedAtMs();
        long pageEndMs = validSamples.getLast().capturedAtMs();
        long pageDurationMs = nonNegativeDuration(pageStartMs, pageEndMs) + SAMPLE_TAIL_MS;
        int pageCharacterCount = page.tokens().stream()
                .mapToInt(this::characterCount)
                .sum();
        if (pageCharacterCount == 0) {
            return PageAnalysis.empty();
        }

        List<Visit> visits = visits(samples);
        MetricAccumulator[] metrics = new MetricAccumulator[page.tokens().size()];
        for (int tokenIndex = 0; tokenIndex < metrics.length; tokenIndex++) {
            metrics[tokenIndex] = new MetricAccumulator();
        }

        boolean[] visited = new boolean[metrics.length];
        int nextExpectedTokenIndex = 0;
        Integer previousTokenIndex = null;
        List<StoryGazeAnalysisResponse.ReplayEvent> events = new ArrayList<>();

        for (Visit visit : visits) {
            int tokenIndex = visit.tokenIndex();
            MetricAccumulator metric = metrics[tokenIndex];
            int durationMs = safeInt(nonNegativeDuration(visit.startedAtMs(), visit.lastSampleAtMs())
                    + SAMPLE_TAIL_MS);
            double expectedDurationMs = (double) pageDurationMs
                    * Math.max(1, characterCount(page.tokens().get(tokenIndex)))
                    / pageCharacterCount;
            boolean dwellQualified = durationMs > expectedDurationMs;
            int eventAtMs = safeInt(Math.max(0L, visit.startedAtMs() - pageStartMs));

            metric.visitCount++;
            metric.firstSeenMs = metric.firstSeenMs == null
                    ? eventAtMs
                    : Math.min(metric.firstSeenMs, eventAtMs);
            metric.skipped = false;
            visited[tokenIndex] = true;
            if (dwellQualified) {
                metric.dwellDurationMs = safeAdd(metric.dwellDurationMs, durationMs);
            }

            StoryGazeAnalysisResponse.MovementType movementType =
                    StoryGazeAnalysisResponse.MovementType.READ;
            List<Integer> skippedTokenIndexes = List.of();
            if (tokenIndex == nextExpectedTokenIndex) {
                nextExpectedTokenIndex++;
            } else if (dwellQualified && tokenIndex > nextExpectedTokenIndex) {
                List<Integer> skipped = new ArrayList<>();
                for (int skippedIndex = nextExpectedTokenIndex;
                     skippedIndex < tokenIndex && skippedIndex < metrics.length;
                     skippedIndex++) {
                    if (visited[skippedIndex]) {
                        continue;
                    }
                    metrics[skippedIndex].skipped = true;
                    skipped.add(skippedIndex);
                }
                skippedTokenIndexes = List.copyOf(skipped);
                movementType = StoryGazeAnalysisResponse.MovementType.SKIP;
                nextExpectedTokenIndex = tokenIndex + 1;
            } else if (dwellQualified && tokenIndex < nextExpectedTokenIndex) {
                movementType = StoryGazeAnalysisResponse.MovementType.REGRESSION;
                metric.regressionCount++;
            }

            events.add(new StoryGazeAnalysisResponse.ReplayEvent(
                    page.page().pageNo(),
                    events.size(),
                    eventAtMs,
                    previousTokenIndex,
                    tokenIndex,
                    movementType,
                    dwellQualified,
                    durationMs,
                    skippedTokenIndexes
            ));
            previousTokenIndex = tokenIndex;
        }

        List<StoryGazeAnalysisResponse.WordMetric> wordMetrics = new ArrayList<>();
        for (int tokenIndex = 0; tokenIndex < metrics.length; tokenIndex++) {
            MetricAccumulator metric = metrics[tokenIndex];
            wordMetrics.add(new StoryGazeAnalysisResponse.WordMetric(
                    page.page().storyLineId(),
                    page.page().pageNo(),
                    tokenIndex,
                    page.tokens().get(tokenIndex),
                    metric.dwellDurationMs,
                    metric.visitCount,
                    metric.skipped,
                    metric.regressionCount,
                    metric.firstSeenMs
            ));
        }
        return new PageAnalysis(wordMetrics, events);
    }

    private List<Visit> visits(List<Sample> samples) {
        List<Visit> result = new ArrayList<>();
        ActiveVisit active = null;
        for (Sample sample : samples) {
            if (sample.tokenIndex() == null) {
                if (active != null) {
                    result.add(active.toVisit());
                    active = null;
                }
                continue;
            }
            boolean continues = active != null
                    && active.tokenIndex == sample.tokenIndex()
                    && sample.capturedAtMs() - active.lastSampleAtMs >= 0
                    && sample.capturedAtMs() - active.lastSampleAtMs <= MAX_SAMPLE_GAP_MS;
            if (continues) {
                active.lastSampleAtMs = sample.capturedAtMs();
                continue;
            }
            if (active != null) {
                result.add(active.toVisit());
            }
            active = new ActiveVisit(
                    sample.tokenIndex(), sample.capturedAtMs(), sample.capturedAtMs()
            );
        }
        if (active != null) {
            result.add(active.toVisit());
        }
        return result;
    }

    private Sample toSample(
            JsonNode source,
            Map<Integer, PageContext> pagesByNo,
            Map<Long, PageContext> pagesByLineId,
            int ordinal
    ) {
        if (source == null || !source.isObject() || !source.hasNonNull("capturedAtMs")) {
            return null;
        }
        long capturedAtMs = source.path("capturedAtMs").asLong(-1L);
        if (capturedAtMs < 0) {
            return null;
        }

        PageContext page = null;
        if (source.hasNonNull("storyLineId")) {
            page = pagesByLineId.get(source.path("storyLineId").asLong());
        }
        int sourcePageNo = source.hasNonNull("pageNo")
                ? source.path("pageNo").asInt(-1)
                : source.path("questionNumber").asInt(-1);
        if (page == null) {
            page = pagesByNo.get(sourcePageNo);
        } else if (sourcePageNo > 0 && sourcePageNo != page.page().pageNo()) {
            return null;
        }
        if (page == null) {
            return null;
        }

        Integer tokenIndex = null;
        if (source.hasNonNull("tokenIndex")) {
            int candidate = source.path("tokenIndex").asInt(-1);
            if (candidate >= 0 && candidate < page.tokens().size()) {
                tokenIndex = candidate;
            }
        }
        return new Sample(page.page().pageNo(), tokenIndex, capturedAtMs, ordinal);
    }

    private JsonNode unwrapRawData(JsonNode stored) {
        JsonNode current = stored;
        for (int depth = 0;
             depth < 4 && current != null && current.path("rawData").isObject();
             depth++) {
            current = current.path("rawData");
        }
        return current;
    }

    private List<String> tokens(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return List.of(source.strip().split("\\s+"));
    }

    private int characterCount(String source) {
        int count = (int) source.codePoints()
                .filter(Character::isLetterOrDigit)
                .count();
        return Math.max(1, count);
    }

    private long nonNegativeDuration(long startMs, long endMs) {
        return Math.max(0L, endMs - startMs);
    }

    private int safeAdd(int left, int right) {
        return safeInt((long) left + right);
    }

    private int safeInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public record Page(long storyLineId, int pageNo, String surfaceText) {
    }

    public record Analysis(
            List<StoryGazeAnalysisResponse.WordMetric> wordMetrics,
            List<StoryGazeAnalysisResponse.ReplayEvent> events
    ) {
        public Analysis {
            wordMetrics = List.copyOf(wordMetrics);
            events = List.copyOf(events);
        }

        private static Analysis empty() {
            return new Analysis(List.of(), List.of());
        }
    }

    private record PageContext(Page page, List<String> tokens) {
        private PageContext {
            tokens = List.copyOf(tokens);
        }
    }

    private record Sample(int pageNo, Integer tokenIndex, long capturedAtMs, int ordinal) {
    }

    private record Visit(int tokenIndex, long startedAtMs, long lastSampleAtMs) {
    }

    private record PageAnalysis(
            List<StoryGazeAnalysisResponse.WordMetric> wordMetrics,
            List<StoryGazeAnalysisResponse.ReplayEvent> events
    ) {
        private PageAnalysis {
            wordMetrics = List.copyOf(wordMetrics);
            events = List.copyOf(events);
        }

        private static PageAnalysis empty() {
            return new PageAnalysis(List.of(), List.of());
        }
    }

    private static final class ActiveVisit {
        private final int tokenIndex;
        private final long startedAtMs;
        private long lastSampleAtMs;

        private ActiveVisit(int tokenIndex, long startedAtMs, long lastSampleAtMs) {
            this.tokenIndex = tokenIndex;
            this.startedAtMs = startedAtMs;
            this.lastSampleAtMs = lastSampleAtMs;
        }

        private Visit toVisit() {
            return new Visit(tokenIndex, startedAtMs, lastSampleAtMs);
        }
    }

    private static final class MetricAccumulator {
        private int dwellDurationMs;
        private int visitCount;
        private boolean skipped;
        private int regressionCount;
        private Integer firstSeenMs;
    }
}
