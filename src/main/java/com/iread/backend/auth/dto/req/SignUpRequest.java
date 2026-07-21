package com.iread.backend.auth.dto.req;

import com.iread.backend.teacher.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Email
        @Size(max = 50)
        String email,

        @NotBlank
        String password,

        @Size(max = 10)
        String name,

        @Size(max = 100)
        String organization,

        Gender gender
) {
}
