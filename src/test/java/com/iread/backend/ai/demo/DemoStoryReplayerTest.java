package com.iread.backend.ai.demo;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.StoryHistoryLine;
import com.iread.backend.ai.dto.req.StoryTemplateData;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.story.domain.StorySceneEntity;
import com.iread.backend.story.domain.StoryTemplateEntity;
import com.iread.backend.story.generation.StorySceneImagePrompt;
import com.iread.backend.story.repository.StoryLineRepository;
import com.iread.backend.story.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoStoryReplayerTest {

    private static final Long SOURCE_STORY_ID = 280003L;
    private static final Long TEMPLATE_ID = 6L;
    private static final String TITLE = "아기돼지 삼형제";

    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final StoryLineRepository storyLineRepository = mock(StoryLineRepository.class);

    private DemoStoryReplayer replayer;

    @BeforeEach
    void setUp() {
        DemoStoryReplayProperties properties =
                new DemoStoryReplayProperties(true, SOURCE_STORY_ID, Duration.ZERO);
        replayer = new DemoStoryReplayer(
                new DemoStoryReplayState(properties),
                properties,
                storyRepository,
                storyLineRepository,
                new JsonMapper()
        );
        stubSourceStory();
    }

    private void stubSourceStory() {
        StoryTemplateEntity template = mock(StoryTemplateEntity.class);
        when(template.getId()).thenReturn(TEMPLATE_ID);
        when(template.getTitle()).thenReturn(TITLE);
        StoryEntity story = mock(StoryEntity.class);
        when(story.getStoryTemplate()).thenReturn(template);
        when(storyRepository.findById(SOURCE_STORY_ID)).thenReturn(Optional.of(story));

        // 원본: 씬1(4페이지, 마지막 분기) + 씬2(5페이지, 마지막 분기) — 시연 1일차 앞 구간과 동일한 구조
        StorySceneEntity scene1 = scene(281004L, "/uploads/images/scene-1.jpg");
        StorySceneEntity scene2 = scene(281005L, "/uploads/images/scene-2.jpg");
        List<StoryLineEntity> lines = List.of(
                line(1L, scene1, "첫째 돼지가 빈터를 살펴보았어요.", null),
                line(2L, scene1, "둘째 돼지가 나무를 모았어요.", null),
                line(3L, scene1, "셋째 돼지가 벽돌을 골랐어요.", null),
                line(4L, scene1, "이제 어떤 집을 먼저 지을까요?", branchPromptJson()),
                line(5L, scene2, "셋째 돼지는 벽돌을 차곡차곡 쌓았어요.", null),
                line(6L, scene2, "형들도 함께 도왔어요.", null),
                line(7L, scene2, "집이 점점 튼튼해졌어요.", null),
                line(8L, scene2, "늑대가 언덕 너머에 나타났어요.", null),
                line(9L, scene2, "다음에는 무엇을 할까요?", branchPromptJson())
        );
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(SOURCE_STORY_ID))
                .thenReturn(lines);
    }

    @Test
    void 토글이_켜져_있으면_시작_요청에_원본_첫_씬을_돌려준다() {
        Optional<GenerateStoryResponse> response = replayer.replayGenerate(generateRequest());

        assertThat(response).isPresent();
        assertThat(response.get().nextProgress()).isEqualTo(4);
        assertThat(response.get().completed()).isFalse();
        assertThat(response.get().lines()).hasSize(4);
        assertThat(response.get().lines().getLast().requiresBranchInput()).isTrue();
        assertThat(response.get().lines().getLast().branchPrompt().options()).hasSize(3);
    }

    @Test
    void 어떤_선택지를_고르든_준비된_다음_씬을_돌려준다() {
        Optional<GenerateStoryResponse> first =
                replayer.replayContinue(continueRequest("벽돌을 차곡차곡 더 쌓아요", 4));
        Optional<GenerateStoryResponse> other =
                replayer.replayContinue(continueRequest("늑대를 찾아 나서요", 4));

        assertThat(first).isPresent();
        assertThat(first.get().nextProgress()).isEqualTo(9);
        assertThat(first.get().lines()).hasSize(5);
        assertThat(other).isPresent();
        assertThat(other.get().lines()).isEqualTo(first.get().lines());
    }

    @Test
    void 구간_경계가_아닌_진행_지점은_실제_생성으로_폴백하도록_비워_돌려준다() {
        assertThat(replayer.replayContinue(continueRequest("벽돌을 차곡차곡 더 쌓아요", 3))).isEmpty();
    }

    @Test
    void 원본_씬_텍스트로_만든_삽화_프롬프트에_원본_이미지를_돌려준다() {
        String sceneText = "첫째 돼지가 빈터를 살펴보았어요. 둘째 돼지가 나무를 모았어요. "
                + "셋째 돼지가 벽돌을 골랐어요. 이제 어떤 집을 먼저 지을까요?";
        GenerateImageRequest request = new GenerateImageRequest(
                "img-1",
                StorySceneImagePrompt.build(TITLE, sceneText),
                TEMPLATE_ID
        );

        var response = replayer.replayImage(request);

        assertThat(response).isPresent();
        assertThat(response.get().imageUrl()).isEqualTo("/uploads/images/scene-1.jpg");
        assertThat(response.get().provider()).isEqualTo(DemoStoryReplayer.PROVIDER);
    }

    @Test
    void 다른_템플릿_요청은_재생하지_않는다() {
        GenerateStoryRequest request = new GenerateStoryRequest(
                "req-1", 1L, 2103L, 1, 0,
                new StoryTemplateData(1L, "토끼와 거북이", "context")
        );

        assertThat(replayer.replayGenerate(request)).isEmpty();
    }

    @Test
    void 토글이_꺼지면_재생하지_않는다() {
        DemoStoryReplayProperties properties =
                new DemoStoryReplayProperties(false, SOURCE_STORY_ID, Duration.ZERO);
        DemoStoryReplayer disabled = new DemoStoryReplayer(
                new DemoStoryReplayState(properties),
                properties,
                storyRepository,
                storyLineRepository,
                new JsonMapper()
        );

        assertThat(disabled.replayGenerate(generateRequest())).isEmpty();
    }

    private GenerateStoryRequest generateRequest() {
        return new GenerateStoryRequest(
                "req-1", 1L, 2103L, 1, 0,
                new StoryTemplateData(TEMPLATE_ID, TITLE, "context")
        );
    }

    private ContinueStoryRequest continueRequest(String branchIntent, int historySize) {
        List<StoryHistoryLine> history = IntStream.rangeClosed(1, historySize)
                .mapToObj(page -> new StoryHistoryLine((long) page, "페이지 " + page, page == historySize))
                .toList();
        return new ContinueStoryRequest(
                "req-2", 1L, 2103L, 1, historySize,
                new StoryTemplateData(TEMPLATE_ID, TITLE, "context"),
                (long) historySize, branchIntent, history
        );
    }

    private StorySceneEntity scene(Long sceneId, String imageUrl) {
        StorySceneEntity scene = mock(StorySceneEntity.class);
        when(scene.getId()).thenReturn(sceneId);
        when(scene.getImageUrl()).thenReturn(imageUrl);
        return scene;
    }

    private StoryLineEntity line(Long id, StorySceneEntity scene, String text, String branchPromptJson) {
        StoryLineEntity line = mock(StoryLineEntity.class);
        when(line.getId()).thenReturn(id);
        when(line.getScene()).thenReturn(scene);
        when(line.getContent()).thenReturn("{\"text\":\"" + text + "\"}");
        when(line.isRequiresBranchInput()).thenReturn(branchPromptJson != null);
        when(line.getBranchPrompt()).thenReturn(branchPromptJson);
        return line;
    }

    private String branchPromptJson() {
        return """
                {"subtitle":"어떤 집을 먼저 지을까요?","options":[
                  {"optionNo":1,"label":"벽돌을 차곡차곡 더 쌓아요"},
                  {"optionNo":2,"label":"나무 울타리를 둘러요"},
                  {"optionNo":3,"label":"지붕부터 얹어요"}
                ]}
                """;
    }
}
