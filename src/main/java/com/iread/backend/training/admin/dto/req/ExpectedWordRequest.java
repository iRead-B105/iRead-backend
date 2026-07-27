package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpectedWordRequest(
        @NotBlank @Size(max = 50) String wordName
) {
}
