package com.iread.backend.student.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iread.backend.student.domain.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
        Long id,
        String name,
        LocalDate birthday,
        Gender gender,
        String school,
        String guardian,
        String guardianContact,
        String guardianEmail,
        Object address,
        String imageUrl,
        String teacherMemo,
        LocalDateTime createdAt
) {
    @JsonProperty("studentCode")
    public String contractStudentCode() {
        return id == null ? null : id.toString();
    }

    @JsonProperty("birthDate")
    public LocalDate contractBirthDate() {
        return birthday;
    }

    @JsonProperty("guardianName")
    public String contractGuardianName() {
        return guardian;
    }

    @JsonProperty("guardianPhone")
    public String contractGuardianPhone() {
        return guardianContact;
    }

    @JsonProperty("profileImage")
    public String contractProfileImage() {
        return imageUrl;
    }

}
