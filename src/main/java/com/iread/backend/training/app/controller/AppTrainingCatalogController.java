package com.iread.backend.training.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.training.app.dto.res.CurrentTrainingListResponse;
import com.iread.backend.training.app.service.AppTrainingCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/training")
public class AppTrainingCatalogController {
    private final AppTrainingCatalogService trainingCatalogService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @GetMapping("/{studentId}")
    public CurrentTrainingListResponse getCurrentTrainingList(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        studentResourceAccessPolicy.requireSameStudent(
                authenticatedStudentId,
                studentId
        );
        return trainingCatalogService.getCurrentTrainingList(teacherId, studentId);
    }
}
