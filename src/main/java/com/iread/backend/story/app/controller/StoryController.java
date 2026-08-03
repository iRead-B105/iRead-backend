package com.iread.backend.story.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.story.app.dto.req.StoryTtsRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.app.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public StoryLinesResponse getStoryLines(@CurrentTeacherId Long teacherId,
                                            @CurrentStudentId Long authenticatedStudentId,
                                            @PathVariable Long studentId,
                                            @PathVariable Long storyId) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.getStoryLines(teacherId, studentId, storyId);
    }

    @Operation(summary = "자연어 선택지를 저장하고 다음 스토리 생성")
    @PostMapping(
            value = "/{studentId}/{storyId}/lines/{lineId}/branches",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public StoryChoiceResponse chooseStoryDirection(@CurrentTeacherId Long teacherId,
                                                    @CurrentStudentId Long authenticatedStudentId,
                                                    @PathVariable Long studentId,
                                                    @PathVariable Long storyId,
                                                    @PathVariable Long lineId,
                                                    @RequestPart("audioFile") MultipartFile audioFile) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.chooseStoryDirection(teacherId, studentId, storyId, lineId, audioFile);
    }

    @Operation(summary = "이야기 문장 음성 인식 및 읽기 정확도 확인")
    @PostMapping(
            value = "/{studentId}/{storyId}/speech",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public StorySpeechResponse transcribeStoryLine(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long storyId,
            @RequestParam("lineId") Long lineId,
            @RequestPart("audioFile") MultipartFile audioFile
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.transcribeStoryLine(teacherId, studentId, storyId, lineId, audioFile);
    }

    @Operation(summary = "이야기 문장 TTS 생성")
    @PostMapping("/{studentId}/{storyId}/tts")
    public StoryTtsResponse synthesizeStoryLine(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long storyId,
            @Valid @RequestBody StoryTtsRequest request
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return storyService.synthesizeStoryLine(teacherId, studentId, storyId, request);
    }

    @GetMapping(value = "/{studentId}/audio/{fileName}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getGeneratedAudio(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable String fileName
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(storyService.getGeneratedAudio(teacherId, studentId, fileName));
    }

    private void authorizeStudent(Long authenticatedStudentId, Long requestedStudentId) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, requestedStudentId);
    }
}
