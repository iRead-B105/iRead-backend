package com.iread.backend.teacher.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.teacher.admin.dto.res.TeacherInfoResponse;
import com.iread.backend.teacher.admin.dto.req.UpdateTeacherProfileRequest;
import com.iread.backend.teacher.admin.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
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

    @Operation(summary = "교사 프로필 수정")
    @PatchMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TeacherInfoResponse updateProfile(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody UpdateTeacherProfileRequest request
    ) {
        return teacherService.updateProfile(teacherId, request);
    }

    @Operation(summary = "교사 프로필 이미지 변경")
    @PatchMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeacherInfoResponse updateProfileImage(
            @CurrentTeacherId Long teacherId,
            @RequestPart("image") MultipartFile image
    ) {
        return teacherService.updateProfileImage(teacherId, image);
    }
}
