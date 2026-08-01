package com.iread.backend.contract;

import com.iread.backend.learning.app.dto.LearningErrorLocation;
import com.iread.backend.student.app.dto.res.LearningEntryResponse;
import com.iread.backend.student.app.dto.res.LearningEntryStatus;
import com.iread.backend.test.app.dto.res.SkillChallengePlanResponse;
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
    void learningEntryExposesChallengeRoutingState() {
        var json = objectMapper.valueToTree(new LearningEntryResponse(
                20L,
                LearningEntryStatus.CHALLENGE_IN_PROGRESS,
                50L,
                4,
                9
        ));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "studentId",
                "entryStatus",
                "testCurriculumId",
                "completedQuestions",
                "totalQuestions"
        );
        assertThat(json.path("entryStatus").asText())
                .isEqualTo("CHALLENGE_IN_PROGRESS");
        assertThat(json.path("completedQuestions").asInt()).isEqualTo(4);
        assertThat(json.path("totalQuestions").asInt()).isEqualTo(9);
    }

    @Test
    void challengePlanExposesOverallNextQuestionAlongsideTracks() {
        var json = objectMapper.valueToTree(new SkillChallengePlanResponse(
                50L,
                4,
                9,
                false,
                105L,
                "short-text",
                List.of()
        ));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "testCurriculumId",
                "completedQuestions",
                "totalQuestions",
                "completed",
                "nextTestId",
                "nextTrackCode",
                "tracks"
        );
        assertThat(json.path("nextTestId").asLong()).isEqualTo(105L);
        assertThat(json.path("nextTrackCode").asText()).isEqualTo("short-text");
    }

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

        assertThat(training.propertyNames()).containsExactlyInAnyOrder(
                "completionType",
                "trainingId",
                "status",
                "completedAt",
                "messageKey",
                "nextAction"
        );
        assertThat(training.has("accuracy")).isFalse();
        assertThat(training.path("messageKey").asText())
                .isEqualTo("TRAINING_COMPLETE_GREAT_JOB");
        assertThat(test.propertyNames()).containsExactlyInAnyOrder(
                "completionType",
                "testId",
                "status",
                "completedAt",
                "messageKey",
                "nextAction"
        );
        assertThat(test.has("accuracy")).isFalse();
        assertThat(test.path("messageKey").asText())
                .isEqualTo("TEST_COMPLETE_GREAT_JOB");
    }
}
