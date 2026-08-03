package com.iread.backend.test.admin.result;

import org.springframework.stereotype.Component;

@Component
public class TestTrackResolver {
    public static final int TOTAL_QUESTIONS = 9;
    public static final int QUESTIONS_PER_TRACK = 3;

    public Track resolve(int sequenceNo) {
        if (sequenceNo < 1 || sequenceNo > TOTAL_QUESTIONS) {
            throw new IllegalStateException("실력도전 검사 순서가 올바르지 않습니다: " + sequenceNo);
        }
        if (sequenceNo <= 3) {
            return new Track("phonological", "음운 인식");
        }
        if (sequenceNo <= 6) {
            return new Track("short-text", "짧은 글");
        }
        return new Track("fluency", "유창성");
    }

    public record Track(String code, String title) {
    }
}
