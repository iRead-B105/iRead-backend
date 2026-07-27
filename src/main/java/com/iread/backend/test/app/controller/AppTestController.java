package com.iread.backend.test.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.test.app.dto.req.*;
import com.iread.backend.test.app.dto.res.*;
import com.iread.backend.test.app.service.AppTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/test/{studentId}")
public class AppTestController {
    private final AppTestService testService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @GetMapping("/intro")
    public TestIntroResponse getIntro(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.getIntro(teacherId, studentId);
    }

    @GetMapping("/questions/{questionNumber}")
    public TestQuestionResponse getQuestion(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable int questionNumber,
            @RequestParam Long testId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.getQuestion(teacherId, studentId, testId, questionNumber);
    }

    @PostMapping("/start")
    public TestStartResponse start(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.start(teacherId, studentId);
    }

    @PostMapping("/session-reset")
    public TestResetResponse reset(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @Valid @RequestBody TestIdRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.reset(teacherId, studentId, request.testId());
    }

    @PostMapping("/questions/{questionNumber}/recordings")
    @ResponseStatus(HttpStatus.CREATED)
    public TestRecordingResponse saveRecording(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable int questionNumber,
            @Valid @RequestBody TestRecordingRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.saveRecording(teacherId, studentId, questionNumber, request);
    }

    @PostMapping("/questions/{questionNumber}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public TestSelectionResponse saveSelection(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable int questionNumber,
            @Valid @RequestBody TestSelectionRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.saveSelection(teacherId, studentId, questionNumber, request);
    }

    @PostMapping("/questions/{questionNumber}/complete")
    public TestQuestionCompleteResponse completeQuestion(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable int questionNumber,
            @Valid @RequestBody TestQuestionCompleteRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.completeQuestion(teacherId, studentId, questionNumber, request);
    }

    @PostMapping("/complete")
    public TestCompleteResponse complete(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @Valid @RequestBody TestCompleteRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return testService.complete(teacherId, studentId, request);
    }

    private void requireSameStudent(Long authenticatedStudentId, Long studentId) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
    }
}
