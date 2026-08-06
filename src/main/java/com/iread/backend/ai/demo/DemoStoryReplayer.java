package com.iread.backend.ai.demo;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.res.GenerateImageResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchPrompt;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.story.generation.StorySceneImagePrompt;
import com.iread.backend.story.repository.StoryLineRepository;
import com.iread.backend.story.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 시연용 스토리 재생기.
 * 토글이 켜져 있으면 qa-demo 데이터셋의 완결본 스토리를 씬 단위 구간으로 잘라
 * AI 생성 응답처럼 돌려준다. 분기 선택지는 그대로 노출하되 어떤 선택을 하든
 * 준비된 다음 구간을 이어간다(시연 중 오클릭이 흐름을 끊지 않게).
 * 구간 경계가 어긋나는 요청만 빈 값을 돌려 실제 AI 생성으로 폴백한다.
 */
@Component
public class DemoStoryReplayer {

    public static final String PROVIDER = "DEMO_STORY_REPLAY";

    private static final Logger log = LoggerFactory.getLogger(DemoStoryReplayer.class);
    private static final String STORY_CHARACTER_PROMPT_PREFIX = "[STORY_CHARACTER]";

    private final DemoStoryReplayState state;
    private final DemoStoryReplayProperties properties;
    private final StoryRepository storyRepository;
    private final StoryLineRepository storyLineRepository;
    private final ObjectMapper objectMapper;

    public DemoStoryReplayer(
            DemoStoryReplayState state,
            DemoStoryReplayProperties properties,
            StoryRepository storyRepository,
            StoryLineRepository storyLineRepository,
            ObjectMapper objectMapper
    ) {
        this.state = state;
        this.properties = properties;
        this.storyRepository = storyRepository;
        this.storyLineRepository = storyLineRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<GenerateStoryResponse> replayGenerate(GenerateStoryRequest request) {
        ReplaySource source = loadSource(request.storyTemplate().storyTemplateId(), request.storyId());
        if (source == null || request.currentProgress() != 0) {
            return Optional.empty();
        }
        return Optional.of(respond(
                request.requestId(),
                request.schemaVersion(),
                source.segments().getFirst()
        ));
    }

    public Optional<GenerateStoryResponse> replayContinue(ContinueStoryRequest request) {
        ReplaySource source = loadSource(request.storyTemplate().storyTemplateId(), request.storyId());
        if (source == null) {
            return Optional.empty();
        }
        ReplaySegment next = source.segmentStartingAt(request.history().size() + 1);
        if (next == null) {
            return Optional.empty();
        }
        return Optional.of(respond(request.requestId(), request.schemaVersion(), next));
    }

    public Optional<GenerateImageResponse> replayImage(GenerateImageRequest request) {
        if (request.prompt() == null
                || request.prompt().strip().startsWith(STORY_CHARACTER_PROMPT_PREFIX)) {
            return Optional.empty();
        }
        ReplaySource source = loadSource(request.storyTemplateId(), null);
        if (source == null) {
            return Optional.empty();
        }
        for (ReplaySegment segment : source.segments()) {
            if (segment.imageUrl() == null || segment.imageUrl().isBlank()) {
                continue;
            }
            String expectedPrompt = StorySceneImagePrompt.build(source.title(), segment.joinedText());
            if (expectedPrompt.equals(request.prompt())) {
                return Optional.of(new GenerateImageResponse(
                        request.requestId(),
                        segment.imageUrl(),
                        PROVIDER
                ));
            }
        }
        return Optional.empty();
    }

    private GenerateStoryResponse respond(String requestId, int schemaVersion, ReplaySegment segment) {
        pause();
        List<GeneratedStoryLine> lines = segment.lines().stream()
                .map(line -> new GeneratedStoryLine(
                        line.text(),
                        line.requiresBranchInput(),
                        line.branchPrompt()
                ))
                .toList();
        return new GenerateStoryResponse(
                requestId,
                schemaVersion,
                segment.endPage(),
                segment.endPage() == 100,
                lines
        );
    }

    private ReplaySource loadSource(Long templateId, Long targetStoryId) {
        if (!state.enabled() || templateId == null) {
            return null;
        }
        Long sourceStoryId = properties.sourceStoryId();
        // 원본 스토리 자신에 대한 요청까지 재생하면 원본이 스스로를 복제하므로 제외한다
        if (Objects.equals(targetStoryId, sourceStoryId)) {
            return null;
        }
        StoryEntity story = storyRepository.findById(sourceStoryId).orElse(null);
        if (story == null || !Objects.equals(story.getStoryTemplate().getId(), templateId)) {
            return null;
        }
        List<StoryLineEntity> lines = storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(sourceStoryId);
        if (lines.isEmpty()) {
            return null;
        }
        try {
            return buildSource(story.getStoryTemplate().getTitle(), lines);
        } catch (RuntimeException exception) {
            log.warn("시연용 원본 스토리를 재생 구간으로 변환하지 못해 실제 AI 생성으로 폴백합니다. sourceStoryId={}",
                    sourceStoryId, exception);
            return null;
        }
    }

    private ReplaySource buildSource(String title, List<StoryLineEntity> masterLines) {
        List<ReplaySegment> segments = new ArrayList<>();
        List<ReplayLine> currentLines = new ArrayList<>();
        Long currentSceneId = null;
        String currentImageUrl = null;
        int startPage = 1;
        for (StoryLineEntity masterLine : masterLines) {
            Long sceneId = masterLine.getScene().getId();
            if (currentSceneId != null && !currentSceneId.equals(sceneId)) {
                segments.add(segment(currentLines, currentImageUrl, startPage));
                startPage += currentLines.size();
                currentLines = new ArrayList<>();
            }
            currentSceneId = sceneId;
            currentImageUrl = masterLine.getScene().getImageUrl();
            currentLines.add(new ReplayLine(
                    textOf(masterLine),
                    masterLine.isRequiresBranchInput(),
                    branchPromptOf(masterLine)
            ));
        }
        segments.add(segment(currentLines, currentImageUrl, startPage));
        return new ReplaySource(title, List.copyOf(segments));
    }

    private ReplaySegment segment(List<ReplayLine> lines, String imageUrl, int startPage) {
        return new ReplaySegment(List.copyOf(lines), imageUrl, startPage, startPage + lines.size() - 1);
    }

    private String textOf(StoryLineEntity line) {
        String text = objectMapper.readTree(line.getContent()).path("text").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("원본 스토리 라인에 본문이 없습니다. lineId=" + line.getId());
        }
        return text;
    }

    private GeneratedStoryBranchPrompt branchPromptOf(StoryLineEntity line) {
        String json = line.getBranchPrompt();
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, GeneratedStoryBranchPrompt.class);
    }

    /** 실시간 생성처럼 보이게 하는 지연. 인터럽트되면 남은 시간을 버리고 바로 반환한다. */
    private void pause() {
        long millis = properties.delay().toMillis();
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record ReplayLine(
            String text,
            boolean requiresBranchInput,
            GeneratedStoryBranchPrompt branchPrompt
    ) {
    }

    private record ReplaySegment(List<ReplayLine> lines, String imageUrl, int startPage, int endPage) {
        String joinedText() {
            return lines.stream().map(ReplayLine::text).collect(Collectors.joining(" "));
        }
    }

    private record ReplaySource(String title, List<ReplaySegment> segments) {
        ReplaySegment segmentStartingAt(int page) {
            return segments.stream()
                    .filter(segment -> segment.startPage() == page)
                    .findFirst()
                    .orElse(null);
        }
    }
}
