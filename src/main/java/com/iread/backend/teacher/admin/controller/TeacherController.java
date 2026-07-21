package com.iread.backend.teacher.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.teacher.admin.dto.res.TeacherInfoResponse;
import com.iread.backend.teacher.admin.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/info")
    public TeacherInfoResponse getTeacherInfo(@CurrentTeacherId Long teacherId) {
        return teacherService.getTeacherInfo(teacherId);
    }
}
