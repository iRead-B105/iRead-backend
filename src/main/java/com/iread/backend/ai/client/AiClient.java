package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;

public interface AiClient {

    GenerateTrainingResponse generateTraining(GenerateTrainingRequest request);
}
