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
                4,
                false,
                List.of(
                        new GeneratedStoryLine(title + "의 문이 살며시 열렸어요.", false),
                        new GeneratedStoryLine("주인공은 반짝이는 길을 따라 천천히 걸었어요.", false),
                        new GeneratedStoryLine("길 끝에서 도움이 필요한 작은 친구를 만났어요.", false),
                        new GeneratedStoryLine("이제 어느 길로 가면 좋을지 말해 볼까요?", true, branchPrompt())
                )
        );
    }

    public GenerateStoryResponse continueStory(ContinueStoryRequest request) {
        int pageCount = request.history().size();
        int pageInDay = pageCount % 10;
        String branchIntent = request.branchIntent().trim();
        List<GeneratedStoryLine> lines;
        if (pageInDay == 4) {
            lines = List.of(
                    new GeneratedStoryLine("샛별이는 ‘" + branchIntent + "’라고 정했고, 이야기에서도 그 선택이 그대로 이루어졌어요.", false),
                    new GeneratedStoryLine("주인공과 친구는 선택한 길을 함께 걸었어요.", false),
                    new GeneratedStoryLine("그 길에서 반짝이는 단서를 발견했어요.", false),
                    new GeneratedStoryLine("단서 덕분에 새로운 장소에 도착했어요.", false),
                    new GeneratedStoryLine("이번에는 어떻게 하면 좋을까요?", true, branchPrompt())
            );
        } else if (pageInDay == 9) {
            boolean finalPage = pageCount == 99;
            lines = List.of(new GeneratedStoryLine(
                    finalPage
                            ? "샛별이의 ‘" + branchIntent + "’ 선택이 이루어지며 긴 모험이 행복하게 끝났어요."
                            : "샛별이의 ‘" + branchIntent + "’ 선택이 이루어지며 오늘의 모험을 잘 마쳤어요.",
                    false
            ));
        } else if (pageInDay == 0 && pageCount < 100) {
            int nextDay = pageCount / 10 + 1;
            lines = List.of(
                    new GeneratedStoryLine(nextDay + "일차 모험의 아침이 밝았어요.", false),
                    new GeneratedStoryLine("주인공은 어제의 선택을 떠올리며 길을 나섰어요.", false),
                    new GeneratedStoryLine("새로운 친구와 함께 다음 단서를 찾았어요.", false),
                    new GeneratedStoryLine("이제 어떤 선택을 하면 좋을까요?", true, branchPrompt())
            );
        } else {
            throw new IllegalArgumentException("스토리 생성 지점은 하루 시작 또는 두 분기 지점이어야 합니다.");
        }
        int nextProgress = pageCount + lines.size();
        boolean completed = nextProgress == 100;
        return new GenerateStoryResponse(
                request.requestId(),
                request.schemaVersion(),
                nextProgress,
                completed,
                lines
        );
    }

    private GeneratedStoryBranchPrompt branchPrompt() {
        return new GeneratedStoryBranchPrompt("별빛 숲의 갈림길", List.of(
                new GeneratedStoryBranchOption(1, "반짝이는 별빛 길로 간다"),
                new GeneratedStoryBranchOption(2, "작은 친구가 가리킨 숲길로 간다"),
                new GeneratedStoryBranchOption(3, "맑은 시냇물 길을 따라간다")
        ));
    }
}
