package com.iread.backend.contract;

import com.iread.backend.student.domain.Gender;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.StudentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StudentContractJsonTest {
    @Autowired ObjectMapper objectMapper;

    @Test
    void acceptsStudentCreateContractFieldAliases() throws Exception {
        StudentRequest request = objectMapper.readValue("""
                {
                  "name": "학생",
                  "studentCode": "ST-001",
                  "birthDate": "2016-03-10",
                  "gender": "Boy",
                  "school": "학교",
                  "guardianName": "보호자",
                  "guardianPhone": "010-0000-0000",
                  "guardianEmail": "guardian@test.com",
                  "address": [{"value": "주소"}],
                  "profileImage": "/images/student.png"
                }
                """, StudentRequest.class);

        assertThat(request.birthday()).isEqualTo(LocalDate.of(2016, 3, 10));
        assertThat(request.guardian()).isEqualTo("보호자");
        assertThat(request.guardianContact()).isEqualTo("010-0000-0000");
        assertThat(request.address()).isInstanceOf(java.util.List.class);
        assertThat(request.imageUrl()).isEqualTo("/images/student.png");
    }

    @Test
    void exposesStudentDetailContractFieldNames() {
        StudentResponse response = new StudentResponse(
                10L,
                "학생",
                LocalDate.of(2016, 3, 10),
                Gender.Boy,
                "학교",
                "보호자",
                "010-0000-0000",
                "guardian@test.com",
                java.util.List.of(java.util.Map.of("value", "주소")),
                "/images/student.png",
                "메모",
                LocalDateTime.of(2026, 7, 27, 12, 0)
        );

        var json = objectMapper.valueToTree(response);
        assertThat(json.get("studentCode").asText()).isEqualTo("10");
        assertThat(json.get("birthDate").asText()).isEqualTo("2016-03-10");
        assertThat(json.get("guardianName").asText()).isEqualTo("보호자");
        assertThat(json.get("guardianPhone").asText()).isEqualTo("010-0000-0000");
        assertThat(json.get("address").isArray()).isTrue();
        assertThat(json.get("profileImage").asText()).isEqualTo("/images/student.png");
        assertThat(json.get("createdAt").asText()).startsWith("2026-07-27T12:00");
    }
}
