package com.iread.backend.training.app.service;

import java.util.Map;

final class KoreanConsonantPronunciation {
    private static final Map<String, String> WITH_EU = Map.ofEntries(
            Map.entry("ㄱ", "그"),
            Map.entry("ㄲ", "끄"),
            Map.entry("ㄴ", "느"),
            Map.entry("ㄷ", "드"),
            Map.entry("ㄸ", "뜨"),
            Map.entry("ㄹ", "르"),
            Map.entry("ㅁ", "므"),
            Map.entry("ㅂ", "브"),
            Map.entry("ㅃ", "쁘"),
            Map.entry("ㅅ", "스"),
            Map.entry("ㅆ", "쓰"),
            Map.entry("ㅇ", "으"),
            Map.entry("ㅈ", "즈"),
            Map.entry("ㅉ", "쯔"),
            Map.entry("ㅊ", "츠"),
            Map.entry("ㅋ", "크"),
            Map.entry("ㅌ", "트"),
            Map.entry("ㅍ", "프"),
            Map.entry("ㅎ", "흐")
    );

    private KoreanConsonantPronunciation() {
    }

    static String withEu(String text) {
        return WITH_EU.getOrDefault(text, text);
    }
}
