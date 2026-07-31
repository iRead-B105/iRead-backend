package com.iread.backend.student.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.student.app.dto.res.GrowthResponse;
import com.iread.backend.student.app.dto.res.AppStudentProfileResponse;
import com.iread.backend.student.app.service.AppStudentProfileService;
import com.iread.backend.student.app.service.GrowthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학생", description = "학습 앱 학생 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/student")
public class AppStudentController {
    private final GrowthService growthService;
    private final AppStudentProfileService appStudentProfileService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @Operation(summary = "현재 아동 프로필 조회")
    @GetMapping("/{studentId}/profile")
    public AppStudentProfileResponse getProfile(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
        return appStudentProfileService.getProfile(teacherId, studentId);
    }

    @Operation(summary = "훈련 템플릿별 성장 정보 조회")
    @GetMapping("/{studentId}/growth")
    public GrowthResponse getGrowth(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId
    ) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
        return growthService.getGrowth(teacherId, studentId);
    }
}
