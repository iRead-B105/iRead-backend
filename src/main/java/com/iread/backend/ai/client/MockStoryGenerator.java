package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockStoryGenerator {

    public GenerateStoryResponse generate(GenerateStoryRequest request) {
        String title = request.storyTemplate().title();
        return new GenerateStoryResponse(
                request.requestId(),
                request.schemaVersion(),
                50,
                false,
                List.of(
                        new GeneratedStoryLine(title + " 이야기가 시작되었어요.", false),
                        new GeneratedStoryLine("주인공은 어디로 가면 좋을까요?", true)
                )
        );
    }

    public GenerateStoryResponse continueStory(ContinueStoryRequest request) {
        int nextProgress = Math.min(100, request.currentProgress() + 50);
        boolean completed = nextProgress == 100;
        String content = completed
                ? request.branchIntent() + " 선택으로 멋진 모험을 마치고 돌아왔어요."
                : request.branchIntent() + " 선택으로 새로운 모험이 펼쳐졌어요.";
        List<GeneratedStoryLine> lines = completed
                ? List.of(new GeneratedStoryLine(content, false))
                : List.of(
                        new GeneratedStoryLine(content, false),
                        new GeneratedStoryLine("다음에는 어떻게 하면 좋을까요?", true)
                );
        return new GenerateStoryResponse(
                request.requestId(),
                request.schemaVersion(),
                nextProgress,
                completed,
                lines
        );
    }
}
