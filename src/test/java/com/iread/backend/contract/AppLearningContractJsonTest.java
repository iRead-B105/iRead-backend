package com.iread.backend.contract;

import com.iread.backend.learning.app.dto.LearningErrorLocation;
import com.iread.backend.test.app.dto.res.TestCompleteResponse;
import com.iread.backend.test.app.dto.res.TestProgressResponse;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.training.app.dto.res.TrainingCompleteResponse;
import com.iread.backend.training.app.dto.res.TrainingFeedbackResponse;
import com.iread.backend.training.domain.TrainingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppLearningContractJsonTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void trainingFeedbackMatchesChildContractBoundary() {
        UUID submissionId = UUID.randomUUID();
        TrainingFeedbackResponse response = new TrainingFeedbackResponse(
                "TRAINING_FEEDBACK",
                submissionId,
                2,
                3,
                1,
                false,
                false,
                true,
                List.of(new LearningErrorLocation(1, null, "INCORRECT_SELECTION")),
                "다시 살펴보세요.",
                null
        );

        var json = objectMapper.valueToTree(response);

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "feedbackType",
                "submissionId",
                "attemptNo",
                "maxAttempts",
                "remainingAttempts",
                "correct",
                "questionCompleted",
                "canRetry",
                "errorLocations",
                "hint",
                "correctResponse"
        );
        assertThat(json.path("submissionId").asText()).isEqualTo(submissionId.toString());
        assertThat(json.path("correctResponse").isNull()).isTrue();
        assertThat(json.has("totalScore")).isFalse();
    }

    @Test
    void testProgressDoesNotExposeCorrectnessOrScore() {
        TestProgressResponse response = new TestProgressResponse(
                "TEST_PROGRESS",
                UUID.randomUUID(),
                true,
                2,
                2,
                5,
                40,
                3,
                false
        );

        var json = objectMapper.valueToTree(response);

        assertThat(json.path("feedbackType").asText()).isEqualTo("TEST_PROGRESS");
        assertThat(json.path("progressPercent").asInt()).isEqualTo(40);
        assertThat(json.has("correct")).isFalse();
        assertThat(json.has("totalScore")).isFalse();
        assertThat(json.has("accuracy")).isFalse();
    }

    @Test
    void completionResponsesExposePraiseOnly() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 28, 15, 0);
        var training = objectMapper.valueToTree(new TrainingCompleteResponse(
                "TRAINING_COMPLETED",
                10L,
                TrainingStatus.COMPLETED,
                completedAt,
                "TRAINING_COMPLETE_GREAT_JOB",
                "RETURN_TO_CURRICULUM"
        ));
        var test = objectMapper.valueToTree(new TestCompleteResponse(
                "TEST_COMPLETED",
                20L,
                TestStatus.COMPLETED,
                completedAt,
                "TEST_COMPLETE_GREAT_JOB",
                "SHOW_COMPLETION"
        ));

        assertThat(training.has("accuracy")).isFalse();
        assertThat(training.path("messageKey").asText())
                .isEqualTo("TRAINING_COMPLETE_GREAT_JOB");
        assertThat(test.has("accuracy")).isFalse();
        assertThat(test.path("messageKey").asText())
                .isEqualTo("TEST_COMPLETE_GREAT_JOB");
    }
}
