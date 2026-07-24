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

    @Column(name = "has_gaze_data", nullable = false)
    private boolean hasGazeData;

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

    @Column(name = "recognized_text", length = 255)
    private String recognizedText;

    @Column(name = "speech_start_offset_ms")
    private Integer speechStartOffsetMs;

    @Column(name = "speech_end_offset_ms")
    private Integer speechEndOffsetMs;

    @Column(name = "is_correct")
    private Boolean correct;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public WordAttemptLogEntity(
            StudentEntity student,
            WordEntity word,
            TrainingEntity training,
            String surfaceText,
            boolean hasGazeData,
            boolean hasAudioData,
            Integer fixationDurationMs,
            Integer fixationCount,
            Integer gazeStartOffsetMs,
            Integer gazeEndOffsetMs,
            Boolean skipped,
            Integer regressionCount,
            String recognizedText,
            Integer speechStartOffsetMs,
            Integer speechEndOffsetMs,
            Boolean correct
    ) {
        this.student = student;
        this.word = word;
        this.training = training;
        this.useLocation = WordAttemptUseLocation.TRAINING;
        this.surfaceText = surfaceText;
        this.hasGazeData = hasGazeData;
        this.hasAudioData = hasAudioData;
        this.fixationDurationMs = fixationDurationMs;
        this.fixationCount = fixationCount;
        this.gazeStartOffsetMs = gazeStartOffsetMs;
        this.gazeEndOffsetMs = gazeEndOffsetMs;
        this.skipped = skipped;
        this.regressionCount = regressionCount;
        this.recognizedText = recognizedText;
        this.speechStartOffsetMs = speechStartOffsetMs;
        this.speechEndOffsetMs = speechEndOffsetMs;
        this.correct = correct;
    }
}
