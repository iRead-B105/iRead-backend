package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FindIdRequest(
        @NotBlank @Size(max = 10) String name,
        @NotBlank @Email @Size(max = 50) String email
) {
}
