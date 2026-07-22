package com.iread.backend.story.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.story.app.dto.req.StoryChoiceRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.app.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스토리", description = "훈련 앱 AI 스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/story")
public class StoryController {

    private final StoryService storyService;

    @Operation(summary = "학생의 스토리 및 템플릿 목록 조회")
    @GetMapping("/{studentId}")
    public StoryShelfResponse getStoryShelf(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return storyService.getStoryShelf(teacherId, studentId);
    }

    @Operation(summary = "스토리 템플릿 상세 조회")
    @GetMapping("/{studentId}/{storyTemplateId}")
    public StoryTemplateResponse getStoryTemplate(@CurrentTeacherId Long teacherId,
                                                  @PathVariable Long studentId,
                                                  @PathVariable Long storyTemplateId) {
        return storyService.getStoryTemplate(teacherId, studentId, storyTemplateId);
    }

    @Operation(summary = "진행 중인 스토리 재개")
    @GetMapping("/{studentId}/{storyId}/resume")
    public StoryResumeResponse resumeStory(@CurrentTeacherId Long teacherId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyId) {
        return storyService.resumeStory(teacherId, studentId, storyId);
    }

    @Operation(summary = "스토리 대사 상세 조회")
    @GetMapping("/{studentId}/{storyId}/lines/{storyLineId}")
    public StoryLineResponse getStoryLine(@CurrentTeacherId Long teacherId,
                                          @PathVariable Long studentId,
                                          @PathVariable Long storyId,
                                          @PathVariable Long storyLineId) {
        return storyService.getStoryLine(teacherId, studentId, storyId, storyLineId);
    }

    @Operation(summary = "새 스토리 세션 시작")
    @PostMapping("/{studentId}/{storyTemplateId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StorySessionResponse startStory(@CurrentTeacherId Long teacherId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyTemplateId) {
        return storyService.startStory(teacherId, studentId, storyTemplateId);
    }

    @Operation(summary = "스토리 대사 목록 조회")
    @GetMapping("/{studentId}/{storyId}/lines")
    public List<StoryLineResponse> getStoryLines(@CurrentTeacherId Long teacherId,
                                                 @PathVariable Long studentId,
                                                 @PathVariable Long storyId) {
        return storyService.getStoryLines(teacherId, studentId, storyId);
    }

    @Operation(summary = "자연어 선택지를 저장하고 다음 스토리 생성")
    @PostMapping("/{studentId}/{storyId}/lines/{storyLineId}/choices")
    public StoryChoiceResponse chooseStoryDirection(@CurrentTeacherId Long teacherId,
                                                    @PathVariable Long studentId,
                                                    @PathVariable Long storyId,
                                                    @PathVariable Long storyLineId,
                                                    @Valid @RequestBody StoryChoiceRequest request) {
        return storyService.chooseStoryDirection(teacherId, studentId, storyId, storyLineId, request);
    }
}
