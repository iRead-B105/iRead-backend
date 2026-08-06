package com.iread.backend.ai.demo;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 시연용 스토리 재생의 런타임 토글.
 * 설정값은 시작값일 뿐이고, 시연 중에는 dev 치트 API로 재시작 없이 켜고 끈다.
 */
@Component
public class DemoStoryReplayState {

    private final AtomicBoolean enabled;

    public DemoStoryReplayState(DemoStoryReplayProperties properties) {
        this.enabled = new AtomicBoolean(properties.enabled());
    }

    public boolean enabled() {
        return enabled.get();
    }

    public boolean toggle() {
        while (true) {
            boolean current = enabled.get();
            if (enabled.compareAndSet(current, !current)) {
                return !current;
            }
        }
    }
}
