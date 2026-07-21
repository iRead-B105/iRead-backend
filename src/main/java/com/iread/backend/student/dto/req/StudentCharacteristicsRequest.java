package com.iread.backend.student.dto.req;

import jakarta.validation.constraints.NotBlank;

public record StudentCharacteristicsRequest(
        @NotBlank String characteristics
) {
}
