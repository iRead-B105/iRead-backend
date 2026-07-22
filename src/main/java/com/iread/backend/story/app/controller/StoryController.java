package com.iread.backend.story.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.story.app.dto.req.StoryChoiceRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.app.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/story")
public class StoryController {

    private final StoryService storyService;

    @GetMapping("/{studentId}")
    public StoryShelfResponse getStoryShelf(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return storyService.getStoryShelf(teacherId, studentId);
    }

    @GetMapping("/{studentId}/{storyTemplateId}")
    public StoryTemplateResponse getStoryTemplate(@CurrentTeacherId Long teacherId,
                                                  @PathVariable Long studentId,
                                                  @PathVariable Long storyTemplateId) {
        return storyService.getStoryTemplate(teacherId, studentId, storyTemplateId);
    }

    @GetMapping("/{studentId}/{storyId}/resume")
    public StoryResumeResponse resumeStory(@CurrentTeacherId Long teacherId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyId) {
        return storyService.resumeStory(teacherId, studentId, storyId);
    }

    @GetMapping("/{studentId}/{storyId}/lines/{storyLineId}")
    public StoryLineResponse getStoryLine(@CurrentTeacherId Long teacherId,
                                          @PathVariable Long studentId,
                                          @PathVariable Long storyId,
                                          @PathVariable Long storyLineId) {
        return storyService.getStoryLine(teacherId, studentId, storyId, storyLineId);
    }

    @PostMapping("/{studentId}/{storyTemplateId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StorySessionResponse startStory(@CurrentTeacherId Long teacherId,
                                           @PathVariable Long studentId,
                                           @PathVariable Long storyTemplateId) {
        return storyService.startStory(teacherId, studentId, storyTemplateId);
    }

    @GetMapping("/{studentId}/{storyId}/lines")
    public List<StoryLineResponse> getStoryLines(@CurrentTeacherId Long teacherId,
                                                 @PathVariable Long studentId,
                                                 @PathVariable Long storyId) {
        return storyService.getStoryLines(teacherId, studentId, storyId);
    }

    @PostMapping("/{studentId}/{storyId}/lines/{storyLineId}/choices")
    public StoryChoiceResponse chooseStoryDirection(@CurrentTeacherId Long teacherId,
                                                    @PathVariable Long studentId,
                                                    @PathVariable Long storyId,
                                                    @PathVariable Long storyLineId,
                                                    @Valid @RequestBody StoryChoiceRequest request) {
        return storyService.chooseStoryDirection(teacherId, studentId, storyId, storyLineId, request);
    }
}
