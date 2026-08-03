package com.iread.backend.gaze.analysis;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GazeDepartureCounterTest {
    private final JsonMapper mapper = new JsonMapper();
    private final GazeDepartureCounter counter = new GazeDepartureCounter();

    @Test
    void countsContiguousAbsenceAtLeastFiveHundredMilliseconds() {
        var data = mapper.readTree("""
                {"samples":[
                  {"offsetMs":100,"presence":false},
                  {"offsetMs":700,"presence":false},
                  {"offsetMs":750,"presence":true}
                ]}
                """);

        assertThat(counter.count(data)).isEqualTo(1);
    }

    @Test
    void countsSeparateDepartureIntervals() {
        var data = mapper.readTree("""
                {"samples":[
                  {"offsetMs":0,"presence":true},
                  {"offsetMs":100,"presence":false},
                  {"offsetMs":600,"presence":true},
                  {"offsetMs":800,"presence":false},
                  {"offsetMs":1400,"presence":false}
                ]}
                """);

        assertThat(counter.count(data)).isEqualTo(2);
    }

    @Test
    void ignoresShortDepartureIntervals() {
        var data = mapper.readTree("""
                {"samples":[
                  {"offsetMs":100,"presence":false},
                  {"offsetMs":499,"presence":true}
                ]}
                """);

        assertThat(counter.count(data)).isZero();
    }

    @Test
    void returnsNullWhenPresenceCannotBeCalculated() {
        var data = mapper.readTree("""
                {"samples":[
                  {"offsetMs":100,"x":0.3,"y":0.4},
                  {"presence":false}
                ]}
                """);

        assertThat(counter.count(data)).isNull();
        assertThat(counter.count(mapper.readTree("{}"))).isNull();
    }
}
