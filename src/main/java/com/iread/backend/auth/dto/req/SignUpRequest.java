package com.iread.backend.auth.dto.req;

import com.iread.backend.validation.SafeText;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
        @NotBlank
        @Email
        @Size(max = 50)
        @SafeText
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @SafeText
        String password,

        @NotBlank
        @Size(max = 10)
        @Pattern(regexp = "^[\\p{L}\\p{M}]+(?:[ .\\-·'’][\\p{L}\\p{M}]+)*$")
        String name,

        @NotBlank
        @Size(max = 100)
        @SafeText
        String organization
) {
}
