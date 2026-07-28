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

        assertThat(generator.generate(request)).isEqualTo(generator.generate(request));
        assertThat(generator.generate(request).nextProgress()).isEqualTo(50);
        assertThat(generator.generate(request).lines().getLast().requiresBranchInput()).isTrue();
    }

    @Test
    void completesStoryAtOneHundredProgress() {
        ContinueStoryRequest request = new ContinueStoryRequest(
                "request-2", 10L, 20L, 1, 50, template, 40L,
                "친구를 따라간다", List.of(new StoryHistoryLine(40L, "어디로 갈까요?", true))
        );

        var response = generator.continueStory(request);

        assertThat(response.completed()).isTrue();
        assertThat(response.nextProgress()).isEqualTo(100);
        assertThat(response.lines().getLast().requiresBranchInput()).isFalse();
    }
}
