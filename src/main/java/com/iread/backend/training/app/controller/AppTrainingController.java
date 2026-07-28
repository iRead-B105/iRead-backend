package com.iread.backend.training.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.training.admin.dto.req.CompleteTrainingRequest;
import com.iread.backend.training.app.dto.req.TrainingRecordingRequest;
import com.iread.backend.training.app.dto.req.TrainingSelectionRequest;
import com.iread.backend.training.app.dto.res.*;
import com.iread.backend.training.app.service.AppTrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/training/{studentId}/{trainingId}")
public class AppTrainingController {
    private final AppTrainingService trainingService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @GetMapping("/intro")
    public TrainingIntroResponse getIntro(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.getIntro(teacherId, studentId, trainingId);
    }

    @GetMapping("/questions/{questionNumber}")
    public TrainingQuestionResponse getQuestion(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @PathVariable int questionNumber
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.getQuestion(teacherId, studentId, trainingId, questionNumber);
    }

    @PostMapping("/start")
    public TrainingStartResponse start(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.start(teacherId, studentId, trainingId);
    }

    @PostMapping("/session-reset")
    public TrainingResetResponse reset(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.reset(teacherId, studentId, trainingId);
    }

    @PostMapping(
            value = "/questions/{questionNumber}/recordings",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingRecordingResponse saveRecording(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @PathVariable int questionNumber,
            @Valid @ModelAttribute TrainingRecordingRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.saveRecording(
                teacherId,
                studentId,
                trainingId,
                questionNumber,
                request
        );
    }

    @PostMapping("/questions/{questionNumber}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingSelectionResponse saveSelection(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @PathVariable int questionNumber,
            @Valid @RequestBody TrainingSelectionRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.saveSelection(
                teacherId,
                studentId,
                trainingId,
                questionNumber,
                request
        );
    }

    @PostMapping("/complete")
    public TrainingCompleteResponse complete(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @Valid @RequestBody CompleteTrainingRequest request
    ) {
        requireSameStudent(authenticatedStudentId, studentId);
        return trainingService.complete(teacherId, studentId, trainingId, request);
    }

    private void requireSameStudent(Long authenticatedStudentId, Long studentId) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
    }
}
