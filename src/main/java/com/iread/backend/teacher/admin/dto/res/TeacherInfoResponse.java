package com.iread.backend.teacher.admin.dto.res;

import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.domain.Gender;

public record TeacherInfoResponse(
        String name,
        String organization,
        String email,
        Gender gender,
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
