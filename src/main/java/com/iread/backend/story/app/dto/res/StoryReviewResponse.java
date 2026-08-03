package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 완료한 이야기를 다시 읽는 화면 응답.
 *
 * <p>대사 전체와 함께 분기마다 학습자가 실제로 고른 답, 완료 보상인 이야기 친구를 담는다.
 * 다시 읽기는 조회일 뿐이므로 읽음 처리나 실시간 이벤트를 일으키지 않는다.
 */
public record StoryReviewResponse(
        Long storyId,
        Long storyTemplateId,
        String title,
        StoryStatus status,
        LocalDateTime createdAt,
        List<StoryLineResponse> storyLines,
        List<BranchChoice> branchChoices,
        StoryFriend storyFriend
) {
    /** 분기 대사에서 학습자가 고른 답. lineId는 분기 대사의 id다. */
    public record BranchChoice(
            Long lineId,
            String selectedText,
            LocalDateTime createdAt
    ) {
    }

    /** 이야기를 완료하며 받은 이야기 친구. 생성에 실패했던 이야기는 null일 수 있다. */
    public record StoryFriend(
            Long characterId,
            String name,
            String imageUrl
    ) {
    }
}
