package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = 50)
        String loginId,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
