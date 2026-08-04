package com.iread.backend.story.app.service;

import com.iread.backend.auth.config.AuthSettings;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoryBranchReviewTokenServiceTest {

    private final StoryBranchReviewTokenService service = new StoryBranchReviewTokenService(
            JsonMapper.builder().build(),
            new AuthSettings(
                    "01234567890123456789012345678901",
                    Duration.ofHours(3),
                    Duration.ofMinutes(5),
                    Duration.ofHours(3),
                    false
            )
    );

    @Test
    void 검토_토큰은_이야기_대사와_STT_원문에_결합된다() {
        String token = service.issue(100L, 1003L, "강을 따라가요", "story-branch-input-v1");

        service.verify(token, 100L, 1003L, "강을 따라가요");

        assertThatThrownBy(() -> service.verify(token, 100L, 1003L, "숲으로 가요"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.verify(token, 100L, 9999L, "강을 따라가요"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 서명이_변조된_검토_토큰을_거부한다() {
        String token = service.issue(100L, 1003L, "강을 따라가요", "story-branch-input-v1");
        String tampered = token.substring(0, token.length() - 1) + "A";

        assertThatThrownBy(() -> service.verify(tampered, 100L, 1003L, "강을 따라가요"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
