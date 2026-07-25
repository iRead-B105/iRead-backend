package com.iread.backend.story.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.story.app.dto.req.StoryChoiceRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.app.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스토리", description = "훈련 앱 AI 스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/story")
public class StoryController {

    private final StoryService storyService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @Operation(summary = "학생의 스토리 및 템플릿 목록 조회")
    @GetMapping("/{studentId}")
    public StoryShelfResponse getStoryShelf(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.getStoryShelf(teacherId, studentId);
    }

    @Operation(summary = "스토리 템플릿 상세 조회")
    @GetMapping("/{studentId}/{storyTemplateId}")
    public StoryTemplateResponse getStoryTemplate(@CurrentTeacherId Long teacherId,
                                                  @CurrentStudentId Long authenticatedStudentId,
                                                  @PathVariable Long studentId,
                                                  @PathVariable Long storyTemplateId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.getStoryTemplate(teacherId, studentId, storyTemplateId);
    }

    @Operation(summary = "진행 중인 스토리 재개")
    @GetMapping("/{studentId}/{storyId}/resume")
    public StoryResumeResponse resumeStory(@CurrentTeacherId Long teacherId,
                                           @CurrentStudentId Long authenticatedStudentId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.resumeStory(teacherId, studentId, storyId);
    }

    @Operation(summary = "스토리 대사 상세 조회")
    @GetMapping("/{studentId}/{storyId}/lines/{lineId}")
    public StoryLineResponse getStoryLine(@CurrentTeacherId Long teacherId,
                                          @CurrentStudentId Long authenticatedStudentId,
                                          @PathVariable Long studentId,
                                          @PathVariable Long storyId,
                                          @PathVariable Long lineId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.getStoryLine(teacherId, studentId, storyId, lineId);
    }

    @Operation(summary = "새 스토리 세션 시작")
    @PostMapping("/{studentId}/{storyTemplateId}/sessions")
    public StorySessionResponse startStory(@CurrentTeacherId Long teacherId,
                                           @CurrentStudentId Long authenticatedStudentId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyTemplateId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.startStory(teacherId, studentId, storyTemplateId);
    }

    @Operation(summary = "스토리 대사 목록 조회")
    @GetMapping("/{studentId}/{storyId}/lines")
    public List<StoryLineResponse> getStoryLines(@CurrentTeacherId Long teacherId,
                                                 @CurrentStudentId Long authenticatedStudentId,
                                                 @PathVariable Long studentId,
                                                 @PathVariable Long storyId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.getStoryLines(teacherId, studentId, storyId);
    }

    @Operation(summary = "자연어 선택지를 저장하고 다음 스토리 생성")
    @PostMapping("/{studentId}/{storyId}/lines/{storyLineId}/choices")
    public StoryChoiceResponse chooseStoryDirection(@CurrentTeacherId Long teacherId,
                                                    @CurrentStudentId Long authenticatedStudentId,
                                                    @PathVariable Long studentId,
                                                    @PathVariable Long storyId,
                                                    @PathVariable Long storyLineId,
                                                    @Valid @RequestBody StoryChoiceRequest request) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.chooseStoryDirection(teacherId, studentId, storyId, storyLineId, request);
    }

    private void authorizeStudent(Long authenticatedStudentId, Long requestedStudentId) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, requestedStudentId);
    }
}
