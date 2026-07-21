package com.iread.backend.student.dto.req;

import com.iread.backend.student.domain.Gender;

import java.time.LocalDate;

public record StudentRequest(
        String name,
        String studentCode,
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
