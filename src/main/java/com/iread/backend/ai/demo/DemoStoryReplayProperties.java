package com.iread.backend.ai.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

/**
 * 시연용 스토리 재생 설정.
 * {@code source-story-id}는 qa-demo 데이터셋이 시드하는 완결본 스토리(아기돼지 삼형제 280003)를
 * 가리키며, 재생 대상 템플릿은 이 스토리의 템플릿에서 유도한다.
 * {@code enabled}는 시작값일 뿐이며 실행 중에는 {@link DemoStoryReplayState}로 켜고 끈다.
 */
@ConfigurationProperties(prefix = "ai.demo-story")
public record DemoStoryReplayProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("280003") Long sourceStoryId,
        @DefaultValue("5s") Duration delay
) {
    public DemoStoryReplayProperties {
        Objects.requireNonNull(sourceStoryId, "ai.demo-story.source-story-id는 필수입니다.");
        Objects.requireNonNull(delay, "ai.demo-story.delay는 필수입니다.");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("ai.demo-story.delay는 음수일 수 없습니다.");
        }
    }
}
