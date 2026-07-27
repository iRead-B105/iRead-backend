package com.iread.backend.student.dto.req;

import com.iread.backend.student.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @Size(max = 10) String name,
        @Past LocalDate birthday,
        Gender gender,
        @Size(max = 20) String school,
        @Size(max = 100) String guardian,
        @Size(max = 20) String guardianContact,
        @Email @Size(max = 50) String guardianEmail,
        @Size(max = 100) String address,
        @Size(max = 255) String imageUrl,
        @Size(max = 5000) String teacherMemo
) {
}
