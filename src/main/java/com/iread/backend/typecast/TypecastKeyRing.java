package com.iread.backend.typecast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typecast API 키 회전기. 활성 키로 호출하다가 할당량 오류(403·429)가
 * 연속 2번 발생하면 다음 키로 자동 전환한다(순환). 401(무효 키)은 카운트
 * 없이 즉시 전환한다. 성공하면 실패 카운터를 리셋한다.
 */
@Component
public class TypecastKeyRing {

    static final int QUOTA_FAILURES_BEFORE_ROTATION = 2;

    private static final Logger log = LoggerFactory.getLogger(TypecastKeyRing.class);

    private final List<String> keys;
    private int activeIndex;
    private int consecutiveQuotaFailures;

    @Autowired
    public TypecastKeyRing(TypecastTtsProperties properties) {
        this(properties.resolvedApiKeys());
    }

    TypecastKeyRing(List<String> keys) {
        this.keys = List.copyOf(keys);
    }

    public synchronized boolean isConfigured() {
        return !keys.isEmpty();
    }

    public synchronized int keyCount() {
        return keys.size();
    }

    /** 현재 활성 키. 키가 없으면 null. */
    public synchronized String activeKey() {
        return keys.isEmpty() ? null : keys.get(activeIndex);
    }

    public synchronized void recordSuccess() {
        consecutiveQuotaFailures = 0;
    }

    /**
     * 할당량 오류를 기록한다. 연속 2번째면 다음 키로 전환한다.
     *
     * @return 키가 전환됐으면 true (호출자는 새 키로 즉시 1회 재시도한다)
     */
    public synchronized boolean recordQuotaFailure() {
        if (keys.size() <= 1) {
            consecutiveQuotaFailures++;
            return false;
        }
        consecutiveQuotaFailures++;
        if (consecutiveQuotaFailures < QUOTA_FAILURES_BEFORE_ROTATION) {
            log.warn(
                    "Typecast 할당량 오류 (키 #{} — 연속 {}회, {}회에 전환)",
                    activeIndex + 1,
                    consecutiveQuotaFailures,
                    QUOTA_FAILURES_BEFORE_ROTATION
            );
            return false;
        }
        rotate("할당량 오류 " + consecutiveQuotaFailures + "회");
        return true;
    }

    /**
     * 무효 키(401)를 기록한다. 죽은 키에 더 낭비하지 않도록 즉시 전환한다.
     *
     * @return 키가 전환됐으면 true
     */
    public synchronized boolean recordInvalidKey() {
        if (keys.size() <= 1) {
            return false;
        }
        rotate("무효 키(401)");
        return true;
    }

    private void rotate(String reason) {
        int previous = activeIndex;
        activeIndex = (activeIndex + 1) % keys.size();
        consecutiveQuotaFailures = 0;
        log.info(
                "Typecast API 키 전환: #{} → #{} (사유: {})",
                previous + 1,
                activeIndex + 1,
                reason
        );
    }
}
