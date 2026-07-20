package com.iread.backend.auth.session;

import com.iread.backend.teacher.domain.TeacherEntity;

import java.io.Serializable;

public record LoginTeacher(
        Long id,
        String email,
        String name,
        String organization,
        Long imagesId
) implements Serializable {

    public static LoginTeacher from(TeacherEntity teacher) {
        return new LoginTeacher(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getOrganization(),
                teacher.getImagesId()
        );
    }
}
