package com.iread.backend.contract;

import com.iread.backend.test.admin.dto.res.TestCurriculumListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminTestContractJsonTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void serializesUnsafeJavaScriptTestCurriculumIdAsExactString() {
        var response = new TestCurriculumListResponse(List.of(
                new TestCurriculumListResponse.Item(
                        1_739_619_061_890_340_497L,
                        "COMPLETED",
                        LocalDateTime.of(2026, 8, 3, 9, 0),
                        LocalDateTime.of(2026, 8, 3, 9, 30),
                        9,
                        9,
                        new BigDecimal("82.50")
                )
        ));

        var json = objectMapper.valueToTree(response);
        var id = json.path("curriculums").get(0).path("testCurriculumId");

        assertThat(id.isTextual()).isTrue();
        assertThat(id.asText()).isEqualTo("1739619061890340497");
    }
}
