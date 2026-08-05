package com.iread.backend.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StudentLoginRequest(
        @NotBlank
        @Pattern(regexp = "\\d+", message = "숫자 형식이어야 합니다.")
        String studentId
) {
}
