package com.iread.backend.readingfeature.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "student_feature_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentFeatureProfileEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_features_id", nullable = false)
    private ReadingFeatureEntity readingFeature;

    @Column(name = "accuracy_rate", precision = 5, scale = 4)
    private BigDecimal accuracyRate;

    @Column(name = "avg_pronunciation_scor")
    private Integer avgPronunciationScore;

    @Column(name = "pronunciation_error_rate", precision = 8, scale = 2)
    private BigDecimal pronunciationErrorRate;

    @Column(name = "avg_fixation_duration_ms")
    private Integer avgFixationDurationMs;

    @Column(name = "avg_fixation_count", precision = 8, scale = 2)
    private BigDecimal avgFixationCount;

    @Column(name = "avg_regression_count", precision = 8, scale = 2)
    private BigDecimal avgRegressionCount;

    @Column(name = "skip_rate", precision = 5, scale = 2)
    private BigDecimal skipRate;

    @Column(name = "avg_reading_time_ms")
    private Integer avgReadingTimeMs;

    @Column(name = "weakness_score")
    private Integer weaknessScore;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "evidence_count")
    private Integer evidenceCount;

    @Column(name = "last_evidence_at")
    private LocalDateTime lastEvidenceAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    public StudentFeatureProfileEntity(Long id, StudentEntity student,
                                       ReadingFeatureEntity readingFeature, BigDecimal confidence) {
        this.id = id;
        this.student = student;
        this.readingFeature = readingFeature;
        this.confidence = confidence;
    }

    public void updateMetrics(BigDecimal accuracyRate, Integer avgPronunciationScore,
                              BigDecimal pronunciationErrorRate, Integer avgFixationDurationMs,
                              BigDecimal avgFixationCount, BigDecimal avgRegressionCount,
                              BigDecimal skipRate, Integer avgReadingTimeMs, Integer weaknessScore,
                              BigDecimal confidence, Integer evidenceCount,
                              LocalDateTime lastEvidenceAt, LocalDateTime analyzedAt) {
        this.accuracyRate = accuracyRate;
        this.avgPronunciationScore = avgPronunciationScore;
        this.pronunciationErrorRate = pronunciationErrorRate;
        this.avgFixationDurationMs = avgFixationDurationMs;
        this.avgFixationCount = avgFixationCount;
        this.avgRegressionCount = avgRegressionCount;
        this.skipRate = skipRate;
        this.avgReadingTimeMs = avgReadingTimeMs;
        this.weaknessScore = weaknessScore;
        this.confidence = confidence;
        this.evidenceCount = evidenceCount;
        this.lastEvidenceAt = lastEvidenceAt;
        this.analyzedAt = analyzedAt;
    }
}
