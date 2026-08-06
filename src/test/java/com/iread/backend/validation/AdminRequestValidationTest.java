package com.iread.backend.validation;

import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.req.UpdateReportMemoRequest;
import com.iread.backend.student.domain.Gender;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.req.UpdateLessonMaterialRequest;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    void rejectsStudentValuesThatDoNotMatchEachFieldIntent() {
        StudentRequest request = new StudentRequest(
                "학생1",
                LocalDate.of(2018, 3, 1),
                Gender.Boy,
                "학교\u0000",
                "보호자1",
                "전화주세요",
                "guardian@example.com",
                List.of(Map.of("unexpected", "주소")),
                null,
                "메모\u0000"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "school", "guardian", "guardianContact", "address", "teacherMemo");
    }

    @Test
    void acceptsOnlyAutoFormattedElevenDigitMobileContact() {
        StudentRequest valid = studentRequestWithContact("010-1234-5678");
        StudentRequest digitsOnly = studentRequestWithContact("01012345678");
        StudentRequest wrongPrefix = studentRequestWithContact("011-1234-5678");
        StudentRequest tooShort = studentRequestWithContact("010-123-4567");

        assertThat(validator.validate(valid))
                .extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("guardianContact");
        assertThat(validator.validate(digitsOnly))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("guardianContact");
        assertThat(validator.validate(wrongPrefix))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("guardianContact");
        assertThat(validator.validate(tooShort))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("guardianContact");
    }

    private StudentRequest studentRequestWithContact(String guardianContact) {
        return new StudentRequest(
                "학생",
                LocalDate.of(2018, 3, 1),
                Gender.Boy,
                "학교",
                "보호자",
                guardianContact,
                "guardian@example.com",
                "서울시",
                null,
                null
        );
    }

    @Test
    void rejectsUnexpectedSignupAndReportValues() {
        SignUpRequest signUp = new SignUpRequest(
                "teacher@example.com", "password\n", "교수자1", "기관\u0000"
        );
        assertThat(validator.validate(signUp))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password", "name", "organization");

        // 미래 날짜 상한은 애노테이션이 아니라 ReportService 가 아동의 학습 날짜 기준으로 검사한다.
        CreateReportRequest report = new CreateReportRequest(
                -1L, LocalDate.now().plusDays(1), LocalDate.now().minusDays(1)
        );
        assertThat(validator.validate(report))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("studentId", "periodOrdered");

        assertThat(validator.validate(new UpdateReportMemoRequest("의견\u0000")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("teacherMemo");
    }

    @Test
    void rejectsInvalidLessonMaterialEnvelopeValues() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UpdateLessonMaterialRequest request = new UpdateLessonMaterialRequest(
                0,
                List.of(new UpdateLessonMaterialRequest.Material(
                        1,
                        "TYPE\u0000",
                        null,
                        mapper.readTree("{}"),
                        mapper.readTree("{}")
                ))
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("materials[0].questionType");
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
    void rejectsNullReportStudentId() {
        CreateReportRequest request = new CreateReportRequest(
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("studentId");
    }
}
