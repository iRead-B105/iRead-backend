package com.iread.backend.story.app.controller;

import com.iread.backend.ai.demo.DemoStoryReplayProperties;
import com.iread.backend.ai.demo.DemoStoryReplayState;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연용 스토리 재생을 실행 중에 켜고 끄는 dev 치트.
 * 학습일 치트와 같은 {@code iread.demo-cheat.enabled} 게이트를 쓴다.
 */
@RestController
@ConditionalOnProperty(name = "iread.demo-cheat.enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/api/app/dev/story-demo")
public class DemoStoryCheatController {

    private final DemoStoryReplayState state;
    private final DemoStoryReplayProperties properties;

    public record DemoStoryReplayStatusResponse(
            boolean enabled,
            Long sourceStoryId,
            long delayMs
    ) {
    }

    @GetMapping
    public DemoStoryReplayStatusResponse status(@CurrentTeacherId Long teacherId) {
        return respond(state.enabled());
    }

    @PostMapping("/toggle")
    public DemoStoryReplayStatusResponse toggle(@CurrentTeacherId Long teacherId) {
        return respond(state.toggle());
    }

    private DemoStoryReplayStatusResponse respond(boolean enabled) {
        return new DemoStoryReplayStatusResponse(
                enabled,
                properties.sourceStoryId(),
                properties.delay().toMillis()
        );
    }
}
