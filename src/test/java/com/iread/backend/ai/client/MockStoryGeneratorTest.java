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
        assertThat(response.nextProgress()).isEqualTo(50);
        assertThat(response.lines()).hasSize(5);
        assertThat(response.lines())
                .extracting(line -> line.requiresBranchInput())
                .containsExactly(false, false, false, false, true);
        assertThat(response.lines().getLast().branchPrompt().options())
                .extracting(option -> option.optionNo())
                .containsExactly(1, 2, 3);
        assertThat(response.lines().subList(0, 4))
                .allMatch(line -> line.branchPrompt() == null);
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
        assertThat(response.lines()).hasSize(5);
        assertThat(response.lines())
                .extracting(line -> line.requiresBranchInput())
                .containsOnly(false);
        assertThat(response.lines())
                .allMatch(line -> line.branchPrompt() == null);
        assertThat(response.lines().getFirst().content()).contains("친구를 따라간다");
    }
}
