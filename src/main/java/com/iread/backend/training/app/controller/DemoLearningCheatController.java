package com.iread.backend.training.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.training.app.dto.res.DemoLearningCheatResponse;
import com.iread.backend.training.app.service.DemoLearningCheatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
@RequiredArgsConstructor
@RequestMapping("/api/app/dev/{studentId}/learning")
public class DemoLearningCheatController {

    private final DemoLearningCheatService cheatService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @PostMapping("/reset")
    public DemoLearningCheatResponse reset(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
        return cheatService.resetProgress(teacherId, studentId);
    }

    @PostMapping("/next-day")
    public DemoLearningCheatResponse nextDay(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
        return cheatService.advanceToNextDay(teacherId, studentId);
    }
}
