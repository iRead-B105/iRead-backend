package com.iread.backend.teacher.admin.dto.req;

import com.iread.backend.teacher.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeacherProfileRequest(
        @NotBlank @Size(max = 10) String name,
        @Size(max = 100) String organization,
        Gender gender
) {
}
