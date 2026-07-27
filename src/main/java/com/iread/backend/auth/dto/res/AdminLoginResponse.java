package com.iread.backend.auth.dto.res;

import com.iread.backend.teacher.domain.TeacherEntity;

public record AdminLoginResponse(
        String teacherId,
        String email,
        String name,
        String organization,
        String profileImage,
        String accessToken,
        String tokenType,
        long expiresIn,
        String loginStatus
) {
    public static AdminLoginResponse completed(
            TeacherEntity teacher,
            String accessToken,
            long expiresIn
    ) {
        return new AdminLoginResponse(
                teacher.getId().toString(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getOrganization(),
                teacher.getImageUrl(),
                accessToken,
                "Bearer",
                expiresIn,
                "COMPLETED"
        );
    }
}
