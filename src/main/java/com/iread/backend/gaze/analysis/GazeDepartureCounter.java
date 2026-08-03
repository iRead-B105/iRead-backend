package com.iread.backend.gaze.analysis;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class GazeDepartureCounter {
    private static final long MIN_DEPARTURE_DURATION_MS = 500L;

    public Integer count(JsonNode data) {
        JsonNode samples = resolveSamples(data);
        if (samples == null || !samples.isArray()) {
            return null;
        }

        List<PresenceSample> validSamples = new ArrayList<>();
        for (JsonNode sample : samples) {
            JsonNode offset = sample.get("offsetMs");
            JsonNode presence = sample.get("presence");
            if (offset == null || !offset.isNumber()
                    || offset.asLong() < 0
                    || presence == null || !presence.isBoolean()) {
                continue;
            }
            validSamples.add(new PresenceSample(offset.asLong(), presence.asBoolean()));
        }
        if (validSamples.isEmpty()) {
            return null;
        }

        validSamples.sort(Comparator.comparingLong(PresenceSample::offsetMs));
        int departures = 0;
        Long departureStartedAt = null;
        for (PresenceSample sample : validSamples) {
            if (!sample.presence() && departureStartedAt == null) {
                departureStartedAt = sample.offsetMs();
            } else if (sample.presence() && departureStartedAt != null) {
                if (sample.offsetMs() - departureStartedAt >= MIN_DEPARTURE_DURATION_MS) {
                    departures++;
                }
                departureStartedAt = null;
            }
        }
        if (departureStartedAt != null) {
            long lastOffset = validSamples.getLast().offsetMs();
            if (lastOffset - departureStartedAt >= MIN_DEPARTURE_DURATION_MS) {
                departures++;
            }
        }
        return departures;
    }

    private JsonNode resolveSamples(JsonNode data) {
        if (data == null || data.isNull()) {
            return null;
        }
        return data.isArray() ? data : data.get("samples");
    }

    private record PresenceSample(long offsetMs, boolean presence) {}
}
