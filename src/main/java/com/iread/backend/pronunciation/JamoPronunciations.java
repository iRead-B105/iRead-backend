package com.iread.backend.pronunciation;

import java.util.Map;

/**
 * 낱자(자모)는 Azure 발음 평가가 기준 텍스트로 정렬하지 못해 0점에 가까운
 * 점수가 나온다. 학습 앱이 아동에게 들려주는 발음(ㄱ→그, ㅏ→아)과 동일한
 * 매핑으로 자모를 실제 발화 음절로 바꿔 평가한다.
 */
public final class JamoPronunciations {

    private static final Map<Character, String> SPOKEN = Map.ofEntries(
            Map.entry('ㄱ', "그"),
            Map.entry('ㄲ', "끄"),
            Map.entry('ㄴ', "느"),
            Map.entry('ㄷ', "드"),
            Map.entry('ㄸ', "뜨"),
            Map.entry('ㄹ', "르"),
            Map.entry('ㅁ', "므"),
            Map.entry('ㅂ', "브"),
            Map.entry('ㅃ', "쁘"),
            Map.entry('ㅅ', "스"),
            Map.entry('ㅆ', "쓰"),
            Map.entry('ㅇ', "으"),
            Map.entry('ㅈ', "즈"),
            Map.entry('ㅉ', "쯔"),
            Map.entry('ㅊ', "츠"),
            Map.entry('ㅋ', "크"),
            Map.entry('ㅌ', "트"),
            Map.entry('ㅍ', "프"),
            Map.entry('ㅎ', "흐"),
            Map.entry('ㅏ', "아"),
            Map.entry('ㅐ', "애"),
            Map.entry('ㅑ', "야"),
            Map.entry('ㅒ', "얘"),
            Map.entry('ㅓ', "어"),
            Map.entry('ㅔ', "에"),
            Map.entry('ㅕ', "여"),
            Map.entry('ㅖ', "예"),
            Map.entry('ㅗ', "오"),
            Map.entry('ㅘ', "와"),
            Map.entry('ㅙ', "왜"),
            Map.entry('ㅚ', "외"),
            Map.entry('ㅛ', "요"),
            Map.entry('ㅜ', "우"),
            Map.entry('ㅝ', "워"),
            Map.entry('ㅞ', "웨"),
            Map.entry('ㅟ', "위"),
            Map.entry('ㅠ', "유"),
            Map.entry('ㅡ', "으"),
            Map.entry('ㅢ', "의"),
            Map.entry('ㅣ', "이")
    );

    private JamoPronunciations() {
    }

    public static String toSpokenText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder spoken = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char letter = text.charAt(index);
            String mapped = SPOKEN.get(letter);
            if (mapped == null) {
                spoken.append(letter);
            } else {
                spoken.append(mapped);
            }
        }
        return spoken.toString();
    }
}
