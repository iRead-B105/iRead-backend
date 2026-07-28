package com.iread.backend.student.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.iread.backend.student.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @Size(max = 10) String name,
        @JsonAlias("birthDate") @Past LocalDate birthday,
        Gender gender,
        @Size(max = 20) String school,
        @JsonAlias("guardianName") @Size(max = 10) String guardian,
        @JsonAlias("guardianPhone") @Size(max = 20) String guardianContact,
        @Email @Size(max = 50) String guardianEmail,
        Object address,
        @JsonAlias("profileImage") @Size(max = 255) String imageUrl,
        @Size(max = 5000) String teacherMemo
) {
}
