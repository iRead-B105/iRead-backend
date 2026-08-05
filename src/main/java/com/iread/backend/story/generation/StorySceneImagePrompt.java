package com.iread.backend.story.generation;

public final class StorySceneImagePrompt {

    private static final String PREFIX = "[STORY_SCENE] ";
    private static final int MAX_LENGTH = 1_000;

    private StorySceneImagePrompt() {
    }

    public static String build(String storyTitle, String sceneText) {
        String prompt = PREFIX
                + "어린이 이야기 '" + storyTitle
                + "'의 장면 삽화. 장면 내용: " + sceneText;
        return prompt.length() <= MAX_LENGTH ? prompt : prompt.substring(0, MAX_LENGTH);
    }
}
