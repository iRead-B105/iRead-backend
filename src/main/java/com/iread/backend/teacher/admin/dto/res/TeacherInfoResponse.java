package com.iread.backend.teacher.admin.dto.res;

import com.iread.backend.teacher.domain.TeacherEntity;

public record TeacherInfoResponse(
        String name,
        String organization,
        String email,
        String gender,
        String profileImageUrl
) {
    public static TeacherInfoResponse from(TeacherEntity teacher, String profileImageUrl) {
        return new TeacherInfoResponse(
                teacher.getName(),
                teacher.getOrganization(),
                teacher.getEmail(),
                teacher.getGender(),
                profileImageUrl
        );
    }
}
