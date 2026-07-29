package com.iread.backend.wordattempt.domain;

import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.WordEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "word_attempt_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordAttemptLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntity word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_line_id")
    private StoryLineEntity storyLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id")
    private TrainingEntity training;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private StudentTestEntity test;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_location", nullable = false, length = 10)
    private WordAttemptUseLocation useLocation;

    @Column(name = "surface_text", length = 50)
    private String surfaceText;

    @Column(name = "has_audio_data", nullable = false)
    private boolean hasAudioData;

    @Column(name = "fixation_duration_ms")
    private Integer fixationDurationMs;

    @Column(name = "fixation_count")
    private Integer fixationCount;

    @Column(name = "gaze_start_offset_ms")
    private Integer gazeStartOffsetMs;

    @Column(name = "gaze_end_offset_ms")
    private Integer gazeEndOffsetMs;

    @Column(name = "is_skipped")
    private Boolean skipped;

    @Column(name = "regression_count")
    private Integer regressionCount;

    @Column(name = "pronunciation_accuracy_score")
    private Integer pronunciationAccuracyScore;

    @Column(name = "speech_start_offset_ms")
    private Integer speechStartOffsetMs;

    @Column(name = "speech_end_offset_ms")
    private Integer speechEndOffsetMs;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "question_no")
    private Integer questionNo;

    @Column(name = "target_index")
    private Integer targetIndex;

    @Column(name = "token_index")
    private Integer tokenIndex;

    @Column(name = "is_final", nullable = false)
    private boolean finalAttempt = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public WordAttemptLogEntity(
            StudentEntity student,
            WordEntity word,
            TrainingEntity training,
            String surfaceText,
            boolean hasAudioData,
            Integer fixationDurationMs,
            Integer fixationCount,
            Integer gazeStartOffsetMs,
            Integer gazeEndOffsetMs,
            Boolean skipped,
            Integer regressionCount,
            Integer pronunciationAccuracyScore,
            Integer speechStartOffsetMs,
            Integer speechEndOffsetMs,
            Boolean correct,
            Integer totalScore,
            Integer questionNo,
            Integer targetIndex,
            Integer tokenIndex,
            boolean finalAttempt
    ) {
        validateScore(pronunciationAccuracyScore, "단어 발음 정확도 점수");
        validateScore(totalScore, "단어 종합 점수");
        validatePosition(questionNo, targetIndex, tokenIndex);
        this.student = student;
        this.word = word;
        this.training = training;
        this.useLocation = WordAttemptUseLocation.TRAINING;
        this.surfaceText = surfaceText;
        this.hasAudioData = hasAudioData;
        this.fixationDurationMs = fixationDurationMs;
        this.fixationCount = fixationCount;
        this.gazeStartOffsetMs = gazeStartOffsetMs;
        this.gazeEndOffsetMs = gazeEndOffsetMs;
        this.skipped = skipped;
        this.regressionCount = regressionCount;
        this.pronunciationAccuracyScore = pronunciationAccuracyScore;
        this.speechStartOffsetMs = speechStartOffsetMs;
        this.speechEndOffsetMs = speechEndOffsetMs;
        this.correct = correct;
        this.totalScore = totalScore;
        this.questionNo = questionNo;
        this.targetIndex = targetIndex;
        this.tokenIndex = tokenIndex;
        this.finalAttempt = finalAttempt;
    }

    public static WordAttemptLogEntity forTest(
            StudentEntity student,
            WordEntity word,
            StudentTestEntity test,
            String surfaceText,
            boolean hasAudioData,
            Integer pronunciationAccuracyScore,
            Integer speechStartOffsetMs,
            Integer speechEndOffsetMs,
            Boolean skipped,
            Boolean correct,
            Integer totalScore,
            Integer questionNo,
            Integer targetIndex,
            Integer tokenIndex
    ) {
        validateScore(pronunciationAccuracyScore, "단어 발음 정확도 점수");
        validateScore(totalScore, "단어 종합 점수");
        validatePosition(questionNo, targetIndex, tokenIndex);
        WordAttemptLogEntity attempt = new WordAttemptLogEntity();
        attempt.student = student;
        attempt.word = word;
        attempt.test = test;
        attempt.useLocation = WordAttemptUseLocation.TEST;
        attempt.surfaceText = surfaceText;
        attempt.hasAudioData = hasAudioData;
        attempt.skipped = skipped;
        attempt.regressionCount = 0;
        attempt.pronunciationAccuracyScore = pronunciationAccuracyScore;
        attempt.speechStartOffsetMs = speechStartOffsetMs;
        attempt.speechEndOffsetMs = speechEndOffsetMs;
        attempt.correct = correct;
        attempt.totalScore = totalScore;
        attempt.questionNo = questionNo;
        attempt.targetIndex = targetIndex;
        attempt.tokenIndex = tokenIndex;
        attempt.finalAttempt = true;
        return attempt;
    }

    public void markNotFinal() {
        this.finalAttempt = false;
    }

    public void applyGazeMetrics(
            Integer fixationDurationMs,
            Integer fixationCount,
            Integer gazeStartOffsetMs,
            Integer gazeEndOffsetMs,
            Boolean gazeSkipped,
            Integer regressionCount,
            Integer totalScore
    ) {
        validateScore(totalScore, "단어 종합 점수");
        this.fixationDurationMs = fixationDurationMs;
        this.fixationCount = fixationCount;
        this.gazeStartOffsetMs = gazeStartOffsetMs;
        this.gazeEndOffsetMs = gazeEndOffsetMs;
        this.skipped = Boolean.TRUE.equals(this.skipped)
                || Boolean.TRUE.equals(gazeSkipped);
        this.regressionCount = regressionCount;
        this.totalScore = totalScore;
    }

    private static void validateScore(Integer score, String label) {
        if (score != null && (score < 0 || score > 1000)) {
            throw new IllegalArgumentException(label + "는 0점 이상 1000점 이하여야 합니다.");
        }
    }

    private static void validatePosition(
            Integer questionNo,
            Integer targetIndex,
            Integer tokenIndex
    ) {
        if (questionNo != null && questionNo < 1) {
            throw new IllegalArgumentException("문항 번호는 1 이상이어야 합니다.");
        }
        if (targetIndex != null && targetIndex < 0) {
            throw new IllegalArgumentException("분석 대상 위치는 0 이상이어야 합니다.");
        }
        if (tokenIndex != null && tokenIndex < 0) {
            throw new IllegalArgumentException("토큰 위치는 0 이상이어야 합니다.");
        }
    }
}
