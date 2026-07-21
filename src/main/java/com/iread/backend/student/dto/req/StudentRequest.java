package com.iread.backend.student.dto.req;

import com.iread.backend.student.domain.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank String name,
        @NotBlank String studentCode,
        LocalDate birthday,
        Gender gender,
        String school,
        String guardian,
        String guardianContact,
        String guardianEmail,
        String address,
        Long imageId
) {
}
