package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 10) String loginId,
        @NotBlank @Email @Size(max = 50) String email,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank String confirmation
) {
}
