package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.StoryHistoryLine;
import com.iread.backend.ai.dto.req.StoryTemplateData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockStoryGeneratorTest {

    private final MockStoryGenerator generator = new MockStoryGenerator();
    private final StoryTemplateData template = new StoryTemplateData(30L, "신비한 숲", "숲 모험");

    @Test
    void returnsDeterministicInitialSegment() {
        GenerateStoryRequest request = new GenerateStoryRequest("request-1", 10L, 20L, 1, 0, template);

        var response = generator.generate(request);

        assertThat(response).isEqualTo(generator.generate(request));
        assertThat(response.nextProgress()).isEqualTo(4);
        assertThat(response.lines()).hasSize(4);
        assertThat(response.lines())
                .extracting(line -> line.requiresBranchInput())
                .containsExactly(false, false, false, true);
    }

    @Test
    void reflectsBranchIntentInFirstDailyContinuation() {
        ContinueStoryRequest request = new ContinueStoryRequest(
                "request-2", 10L, 20L, 1, 4, template, 40L,
                "토끼가 이긴다", List.of(
                        new StoryHistoryLine(37L, "첫 장면", false),
                        new StoryHistoryLine(38L, "둘째 장면", false),
                        new StoryHistoryLine(39L, "셋째 장면", false),
                        new StoryHistoryLine(40L, "어디로 갈까요?", true)
                )
        );

        var response = generator.continueStory(request);

        assertThat(response.completed()).isFalse();
        assertThat(response.nextProgress()).isEqualTo(9);
        assertThat(response.lines()).hasSize(5);
        assertThat(response.lines())
                .extracting(line -> line.requiresBranchInput())
                .containsExactly(false, false, false, false, true);
        assertThat(response.lines().getFirst().content()).contains("토끼가 이긴다");
    }
}
