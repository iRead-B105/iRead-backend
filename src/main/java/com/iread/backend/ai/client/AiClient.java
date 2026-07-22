package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;

public interface AiClient {

    GenerateTrainingResponse generateTraining(GenerateTrainingRequest request);

    GenerateStoryResponse generateStory(GenerateStoryRequest request);

    GenerateStoryResponse continueStory(ContinueStoryRequest request);
}
