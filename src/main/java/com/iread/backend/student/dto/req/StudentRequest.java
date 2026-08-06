package com.iread.backend.student.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.iread.backend.student.domain.Gender;
import com.iread.backend.validation.SafeText;
import com.iread.backend.validation.ValidAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @Size(min = 1, max = 10)
        @Pattern(regexp = "^[\\p{L}\\p{M}]+(?:[ .\\-·'’][\\p{L}\\p{M}]+)*$")
        String name,
        @JsonAlias("birthDate") @Past LocalDate birthday,
        Gender gender,
        @Size(min = 1, max = 20) @SafeText @Pattern(regexp = ".*\\S.*") String school,
        @JsonAlias("guardianName")
        @Size(min = 1, max = 10)
        @Pattern(regexp = "^[\\p{L}\\p{M}]+(?:[ .\\-·'’][\\p{L}\\p{M}]+)*$")
        String guardian,
        @JsonAlias("guardianPhone")
        @Size(min = 13, max = 13)
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
        String guardianContact,
        @Email @Size(max = 50) String guardianEmail,
        @ValidAddress Object address,
        @JsonAlias("profileImage") @Size(max = 255) @SafeText String imageUrl,
        @Size(max = 1000) @SafeText(allowLineBreaks = true) String teacherMemo
) {
}
