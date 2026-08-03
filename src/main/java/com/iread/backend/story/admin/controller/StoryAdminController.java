package com.iread.backend.story.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.story.admin.dto.res.StoryGazeAnalysisResponse;
import com.iread.backend.story.admin.dto.res.StoryHistoryDetailResponse;
import com.iread.backend.story.admin.dto.res.StoryHistoryResponse;
import com.iread.backend.story.admin.dto.req.StoryPageImageRegenerateRequest;
import com.iread.backend.story.admin.dto.req.StoryPageUpdateRequest;
import com.iread.backend.story.admin.dto.res.StoryPageEditResponse;
import com.iread.backend.story.admin.service.StoryAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.time.LocalDate;

@Tag(name = "Story admin", description = "Teacher story history and gaze analysis APIs")
@RestController
@RequiredArgsConstructor
public class StoryAdminController {

    private final StoryAdminService storyAdminService;

    @Operation(summary = "Get student story history")
    @GetMapping("/api/admin/student/{studentId}/story-history")
    public StoryHistoryResponse getStoryHistory(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long storyTemplateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return storyAdminService.getStoryHistory(
                teacherId, studentId, from, to, storyTemplateId, page, size
        );
    }

    @Operation(summary = "Get student story history detail")
    @GetMapping("/api/admin/student/{studentId}/story-history/{storyId}")
    public StoryHistoryDetailResponse getStoryHistoryDetail(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long storyId
    ) {
        return storyAdminService.getStoryHistoryDetail(teacherId, studentId, storyId);
    }

    @Operation(summary = "Update an unread generated story page")
    @PutMapping("/api/admin/student/{studentId}/story-history/{storyId}/pages/{storyLineId}")
    public StoryPageEditResponse updateUnreadPage(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long storyId,
            @PathVariable Long storyLineId,
            @Valid @RequestBody StoryPageUpdateRequest request
    ) {
        return storyAdminService.updateUnreadPage(
                teacherId, studentId, storyId, storyLineId, request
        );
    }

    @Operation(summary = "Upload an image for an unread generated story page")
    @PostMapping("/api/admin/student/{studentId}/story-history/{storyId}/pages/{storyLineId}/image")
    public StoryPageEditResponse uploadUnreadPageImage(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long storyId,
            @PathVariable Long storyLineId,
            @RequestPart Long revision,
            @RequestPart MultipartFile image
    ) {
        return storyAdminService.uploadUnreadPageImage(
                teacherId, studentId, storyId, storyLineId, revision, image
        );
    }

    @Operation(summary = "Regenerate an image for an unread generated story page")
    @PostMapping("/api/admin/student/{studentId}/story-history/{storyId}/pages/{storyLineId}/image/regenerate")
    public StoryPageEditResponse regenerateUnreadPageImage(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long storyId,
            @PathVariable Long storyLineId,
            @Valid @RequestBody StoryPageImageRegenerateRequest request
    ) {
        return storyAdminService.regenerateUnreadPageImage(
                teacherId, studentId, storyId, storyLineId, request.revision()
        );
    }

    @Operation(summary = "Get story gaze analysis")
    @GetMapping("/api/admin/story/{studentId}/{storyId}/gaze-analysis")
    public StoryGazeAnalysisResponse getStoryGazeAnalysis(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long storyId
    ) {
        return storyAdminService.getStoryGazeAnalysis(teacherId, studentId, storyId);
    }
}
