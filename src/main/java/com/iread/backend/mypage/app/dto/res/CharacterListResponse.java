package com.iread.backend.mypage.app.dto.res;

import java.util.List;

public record CharacterListResponse(
        List<CharacterResponse> characters
) {
}
