package com.iread.backend.auth.dto;

import com.iread.backend.auth.session.LoginTeacher;
import com.iread.backend.teacher.domain.TeacherEntity;

public record TeacherAuthResponse(
        Long id,
        String email,
        String name,
        String organization,
        Long imagesId
) {

    public static TeacherAuthResponse from(TeacherEntity teacher) {
        return new TeacherAuthResponse(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getOrganization(),
                teacher.getImagesId()
        );
    }

    public static TeacherAuthResponse from(LoginTeacher teacher) {
        return new TeacherAuthResponse(
                teacher.id(),
                teacher.email(),
                teacher.name(),
                teacher.organization(),
                teacher.imagesId()
        );
    }
}
