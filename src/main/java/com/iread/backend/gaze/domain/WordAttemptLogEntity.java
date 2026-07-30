package com.iread.backend.gaze.domain;

import jakarta.persistence.*;
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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gaze_analysis_result_id", nullable = false)
    private GazeAnalysisResultEntity gazeAnalysisResult;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "word_id")
    private Long wordId;

    @Column(name = "story_line_id")
    private Long storyLineId;

    @Column(name = "training_id")
    private Long trainingId;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "word_index", nullable = false)
    private Integer wordIndex;

    @Column(name = "surface_text", nullable = false, length = 100)
    private String surfaceText;

    @Column(name = "has_audio_data")
    private Boolean hasAudioData;

    @Column(name = "fixation_duration_ms")
    private Integer fixationDurationMs;

    @Column(name = "fixation_count")
    private Integer fixationCount;

    @Column(name = "gaze_start_offset_ms")
    private Integer gazeStartOffsetMs;

    @Column(name = "gaze_end_offset_ms")
    private Integer gazeEndOffsetMs;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "is_fixated")
    private Boolean isFixated;

    @Column(name = "is_skipped")
    private Boolean isSkipped;

    @Column(name = "is_regressed")
    private Boolean isRegressed;

    @Column(name = "regression_count")
    private Integer regressionCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WordAttemptLogEntity(GazeAnalysisResultEntity gazeAnalysisResult, Long studentId, Long wordId,
                                Long storyLineId, Long trainingId, Long testId, Integer wordIndex,
                                String surfaceText, Boolean hasAudioData, Integer fixationDurationMs,
                                Integer fixationCount, Integer gazeStartOffsetMs, Integer gazeEndOffsetMs,
                                Boolean isRead, Boolean isFixated, Boolean isSkipped, Boolean isRegressed,
                                Integer regressionCount) {
        this.gazeAnalysisResult = gazeAnalysisResult;
        this.studentId = studentId;
        this.wordId = wordId;
        this.storyLineId = storyLineId;
        this.trainingId = trainingId;
        this.testId = testId;
        this.wordIndex = wordIndex;
        this.surfaceText = surfaceText;
        this.hasAudioData = hasAudioData;
        this.fixationDurationMs = fixationDurationMs;
        this.fixationCount = fixationCount;
        this.gazeStartOffsetMs = gazeStartOffsetMs;
        this.gazeEndOffsetMs = gazeEndOffsetMs;
        this.isRead = isRead;
        this.isFixated = isFixated;
        this.isSkipped = isSkipped;
        this.isRegressed = isRegressed;
        this.regressionCount = regressionCount;
    }
}
