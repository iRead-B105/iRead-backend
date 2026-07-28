package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AiClient {

    GenerateTrainingResponse generateTraining(GenerateTrainingRequest request);

    EvaluateTrainingResponse evaluateTraining(EvaluateTrainingRequest request);

    GenerateStoryResponse generateStory(GenerateStoryRequest request);

    GenerateStoryResponse continueStory(ContinueStoryRequest request);

    SpeechTranscriptionResponse transcribeSpeech(
            String requestId, Long studentId, String expectedText, MultipartFile audioFile
    );

    SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request);
}
