package com.iread.backend.mypage.app.dto.res;

import java.time.LocalDateTime;

public record CharacterResponse(
        Long characterId,
        Long storyId,
        String imageUrl,
        String name,
        LocalDateTime createdAt
) {
}
