package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
