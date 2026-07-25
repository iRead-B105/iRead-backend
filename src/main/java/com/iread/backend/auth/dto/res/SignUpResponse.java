package com.iread.backend.auth.dto.res;

import com.iread.backend.teacher.domain.TeacherEntity;

public record SignUpResponse(
        String teacherId,
        String loginId,
        String email,
        String signUpStatus
) {
    public static SignUpResponse completed(TeacherEntity teacher) {
        return new SignUpResponse(
                teacher.getId().toString(),
                teacher.getLoginId(),
                teacher.getEmail(),
                "COMPLETED"
        );
    }
}
