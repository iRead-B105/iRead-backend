package com.iread.backend.student.app.dto.res;

import com.iread.backend.student.domain.StudentEntity;

import java.time.LocalDate;
import java.time.Period;

public record AppStudentProfileResponse(
        String studentId,
        String name,
        Integer age,
        String profileImageUrl
) {
    public static AppStudentProfileResponse from(StudentEntity student, LocalDate today) {
        Integer age = student.getBirthday() == null
                ? null
                : Period.between(student.getBirthday(), today).getYears();
        return new AppStudentProfileResponse(
                student.getId().toString(),
                student.getName(),
                age,
                student.getImageUrl()
        );
    }
}
