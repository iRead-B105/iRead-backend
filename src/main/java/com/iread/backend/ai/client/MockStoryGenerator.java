package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchOption;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchPrompt;
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
                        new GeneratedStoryLine(title + "의 문이 살며시 열렸어요.", false),
                        new GeneratedStoryLine("주인공은 반짝이는 길을 따라 천천히 걸었어요.", false),
                        new GeneratedStoryLine("길 끝에서 도움이 필요한 작은 친구를 만났어요.", false),
                        new GeneratedStoryLine("두 친구는 힘을 합쳐 숨겨진 표지판을 찾아냈어요.", false),
                        new GeneratedStoryLine("이제 어느 길로 가면 좋을까요?", true, branchPrompt())
                )
        );
    }

    public GenerateStoryResponse continueStory(ContinueStoryRequest request) {
        int nextProgress = Math.min(100, request.currentProgress() + 50);
        boolean completed = nextProgress == 100;
        String branchIntent = request.branchIntent();
        List<GeneratedStoryLine> lines = completed
                ? List.of(
                        new GeneratedStoryLine("주인공은 \"" + branchIntent + "\"라고 말했어요.", false),
                        new GeneratedStoryLine("작은 친구는 고개를 끄덕이며 앞장섰어요.", false),
                        new GeneratedStoryLine("두 친구는 선택한 길에서 빛나는 단서를 발견했어요.", false),
                        new GeneratedStoryLine("단서를 따라가자 잃어버린 보물이 모습을 드러냈어요.", false),
                        new GeneratedStoryLine("모두 함께 기뻐하며 멋진 모험을 마치고 돌아왔어요.", false)
                )
                : List.of(
                        new GeneratedStoryLine(branchIntent + " 선택으로 새로운 모험이 펼쳐졌어요.", false),
                        new GeneratedStoryLine("새로운 길에서 또 다른 친구를 만났어요.", false),
                        new GeneratedStoryLine("친구와 함께 어려운 문제를 해결했어요.", false),
                        new GeneratedStoryLine("멀리서 새로운 표지판이 반짝였어요.", false),
                        new GeneratedStoryLine("다음에는 어떻게 하면 좋을까요?", true, branchPrompt())
                );
        return new GenerateStoryResponse(
                request.requestId(),
                request.schemaVersion(),
                nextProgress,
                completed,
                lines
        );
    }

    private GeneratedStoryBranchPrompt branchPrompt() {
        return new GeneratedStoryBranchPrompt(List.of(
                new GeneratedStoryBranchOption(1, "반짝이는 별빛 길로 간다"),
                new GeneratedStoryBranchOption(2, "작은 친구가 가리킨 숲길로 간다"),
                new GeneratedStoryBranchOption(3, "맑은 시냇물 길을 따라간다")
        ));
    }
}
