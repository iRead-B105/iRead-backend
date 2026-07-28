package com.iread.backend.validation;

import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.student.domain.Gender;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRequestValidationTest {
    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void rejectsInvalidStudentProfileFields() {
        StudentRequest request = new StudentRequest(
                "이름이열글자를초과하는학생",
                LocalDate.now().plusDays(1),
                Gender.Boy,
                "학교",
                "보호자",
                "010-1234-5678",
                "invalid-email",
                "주소",
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "birthday", "guardianEmail");
    }

    @Test
    void rejectsNullTrainingTemplateIdInsideCurriculum() {
        UpdateCurriculumRequest request = new UpdateCurriculumRequest(
                Arrays.asList(1L, null)
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("trainingTemplateIds[1].<list element>");
    }

    @Test
    void rejectsOversizedReportMemo() {
        CreateReportRequest request = new CreateReportRequest(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "가".repeat(5001)
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("teacherMemo");
    }
}
