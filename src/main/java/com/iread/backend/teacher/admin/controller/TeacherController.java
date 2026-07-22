package com.iread.backend.teacher.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.teacher.admin.dto.res.TeacherInfoResponse;
import com.iread.backend.teacher.admin.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "교사", description = "관리자 앱 교사 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "현재 로그인한 교사 정보 조회")
    @GetMapping("/info")
    public TeacherInfoResponse getTeacherInfo(@CurrentTeacherId Long teacherId) {
        return teacherService.getTeacherInfo(teacherId);
    }
}
