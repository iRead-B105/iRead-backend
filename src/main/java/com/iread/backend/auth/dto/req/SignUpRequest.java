package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Email
        @Size(max = 50)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 10)
        String name,

        @NotBlank
        @Size(max = 100)
        String organization
) {
}
