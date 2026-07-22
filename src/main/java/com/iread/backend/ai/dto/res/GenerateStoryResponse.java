package com.iread.backend.ai.dto.res;

import java.util.List;

public record GenerateStoryResponse(
        String requestId,
        int schemaVersion,
        boolean completed,
        List<GeneratedStoryLine> lines
) {
}
