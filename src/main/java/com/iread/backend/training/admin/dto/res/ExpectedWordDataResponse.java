package com.iread.backend.training.admin.dto.res;

import java.util.List;

public record ExpectedWordDataResponse(
        List<ExpectedWordResponse> words
) {
}
