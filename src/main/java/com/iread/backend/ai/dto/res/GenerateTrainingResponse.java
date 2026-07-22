package com.iread.backend.ai.dto.res;

import tools.jackson.databind.JsonNode;

public record GenerateTrainingResponse(
        String requestId,
        int schemaVersion,
        JsonNode generatedData
) {
}
