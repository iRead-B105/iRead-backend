package com.iread.backend.contract;

import com.iread.backend.mypage.app.dto.res.CharacterListResponse;
import com.iread.backend.mypage.app.dto.res.CharacterResponse;
import com.iread.backend.student.app.dto.res.GrowthAreaResponse;
import com.iread.backend.student.app.dto.res.GrowthResponse;
import com.iread.backend.student.app.dto.res.TrainingProgressResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppStudentContractJsonTest {
    @Autowired ObjectMapper objectMapper;

    @Test
    void exposesCharacterListContractFields() {
        CharacterListResponse response = new CharacterListResponse(List.of(
                new CharacterResponse(
                        10L,
                        20L,
                        "/characters/book-fairy.png",
                        "책 요정",
                        LocalDateTime.of(2026, 7, 27, 12, 0)
                )
        ));

        var json = objectMapper.valueToTree(response);
        var character = json.get("characters").get(0);

        assertThat(character.get("characterId").asLong()).isEqualTo(10L);
        assertThat(character.get("storyId").asLong()).isEqualTo(20L);
        assertThat(character.get("imageUrl").asText()).isEqualTo("/characters/book-fairy.png");
        assertThat(character.get("name").asText()).isEqualTo("책 요정");
        assertThat(character.get("createdAt").asText()).startsWith("2026-07-27T12:00");
    }

    @Test
    void exposesTrainingProgressContractFields() {
        GrowthResponse response = new GrowthResponse(List.of(
                new TrainingProgressResponse(30L, "낱말 읽기", 3L)
        ), List.of(
                new GrowthAreaResponse(
                        2,
                        "읽기",
                        3,
                        "꽃봉오리",
                        8,
                        3,
                        8,
                        38,
                        2,
                        25,
                        new BigDecimal("75.50"),
                        LocalDateTime.of(2026, 7, 29, 12, 0)
                )
        ));

        var json = objectMapper.valueToTree(response);
        var progress = json.get("trainingProgress").get(0);
        var area = json.get("growthAreas").get(0);

        assertThat(progress.get("trainingTemplateId").asLong()).isEqualTo(30L);
        assertThat(progress.get("trainingTemplateName").asText()).isEqualTo("낱말 읽기");
        assertThat(progress.get("completedCount").asLong()).isEqualTo(3L);
        assertThat(area.get("areaId").asInt()).isEqualTo(2);
        assertThat(area.get("stage").asInt()).isEqualTo(3);
        assertThat(area.get("stageName").asText()).isEqualTo("꽃봉오리");
        assertThat(area.get("recentAverageAccuracy").decimalValue())
                .isEqualByComparingTo("75.50");
    }
}
