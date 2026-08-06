package com.iread.backend.story.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 이야기 분기 생성이 같은 문장에서 두 번 돌지 않게 막는 잠금.
 *
 * <p>선택 기록은 AI 생성이 끝난 뒤에 저장되므로, 생성 중에는 데이터베이스만 봐서는
 * 진행 여부를 알 수 없다. 그 사이 아이가 홈으로 나갔다 돌아와 같은 선택지를 다시 누르면
 * 같은 분기로 생성이 두 번 돌아간다. 생성이 긴 트랜잭션 안에서 일어나 커밋 전에는
 * 다른 요청이 볼 수 없기 때문에, 트랜잭션과 무관한 Redis 에 잠금을 둔다.
 */
@Component
public class StoryBranchGenerationLock {

    private static final Logger log = LoggerFactory.getLogger(StoryBranchGenerationLock.class);
    private static final String KEY_PREFIX = "iread:story:branch-generating:";

    /**
     * AI 읽기 제한(기본 210s)보다 길게 잡아 생성 중에 잠금이 먼저 풀리지 않게 한다.
     * 서버가 죽어 해제를 못 해도 이 시간이 지나면 분기가 다시 열린다.
     */
    private static final Duration TTL = Duration.ofSeconds(240);

    private final StringRedisTemplate redisTemplate;

    public StoryBranchGenerationLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 잠금을 얻으면 true. 이미 생성 중이면 false. */
    public boolean tryAcquire(Long storyLineId) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key(storyLineId), "1", TTL)
            );
        } catch (RuntimeException exception) {
            // Redis 가 없다고 이야기 진행 자체를 막지는 않는다. 중복 생성 방지는
            // 편의 기능이고, 중복이 생겨도 선택 기록 갱신으로 결과는 하나로 수렴한다.
            log.warn("story branch lock acquire skipped: {}", exception.getMessage());
            return true;
        }
    }

    public void release(Long storyLineId) {
        try {
            redisTemplate.delete(key(storyLineId));
        } catch (RuntimeException exception) {
            // 해제에 실패해도 TTL 이 정리한다.
            log.warn("story branch lock release skipped: {}", exception.getMessage());
        }
    }

    /** 이 문장의 다음 장면이 지금 생성되고 있는가. */
    public boolean isGenerating(Long storyLineId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(storyLineId)));
        } catch (RuntimeException exception) {
            log.warn("story branch lock check skipped: {}", exception.getMessage());
            return false;
        }
    }

    private String key(Long storyLineId) {
        return KEY_PREFIX + storyLineId;
    }
}
