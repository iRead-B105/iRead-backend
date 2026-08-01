package com.iread.backend.test.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "test_curriculums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestCurriculumEntity {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NOT_REQUESTED'")
    @Column(name = "recommendation_status", nullable = false, length = 20)
    private TestRecommendationStatus recommendationStatus =
            TestRecommendationStatus.NOT_REQUESTED;

    @Column(name = "recommendation_error", length = 2000)
    private String recommendationError;

    @Column(name = "recommendation_last_attempt_at")
    private LocalDateTime recommendationLastAttemptAt;

    @ColumnDefault("0")
    @Column(name = "recommendation_retry_count", nullable = false)
    private int recommendationRetryCount;

    public TestCurriculumEntity(Long id, StudentEntity student, LocalDateTime createdAt) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("검사 커리큘럼 ID는 1 이상이어야 합니다.");
        }
        this.id = id;
        this.student = student;
        this.status = TestStatus.NOT_STARTED.name();
        this.createdAt = createdAt;
    }

    public void start() {
        if (TestStatus.NOT_STARTED.name().equals(status)) {
            this.status = TestStatus.IN_PROGRESS.name();
        }
    }

    public boolean complete(LocalDateTime completedAt) {
        if (TestStatus.COMPLETED.name().equals(status)) {
            return false;
        }
        this.status = TestStatus.COMPLETED.name();
        this.completedAt = completedAt;
        this.recommendationStatus = TestRecommendationStatus.PENDING;
        this.recommendationError = null;
        return true;
    }

    public boolean startRecommendation(LocalDateTime attemptedAt) {
        if (recommendationStatus != TestRecommendationStatus.PENDING
                && recommendationStatus != TestRecommendationStatus.FAILED) {
            return false;
        }
        recommendationStatus = TestRecommendationStatus.PROCESSING;
        recommendationError = null;
        recommendationLastAttemptAt = attemptedAt;
        recommendationRetryCount++;
        return true;
    }

    public void completeRecommendation() {
        recommendationStatus = TestRecommendationStatus.COMPLETED;
        recommendationError = null;
    }

    public void failRecommendation(String error) {
        recommendationStatus = TestRecommendationStatus.FAILED;
        recommendationError = error == null
                ? "알 수 없는 추천 처리 오류"
                : error.substring(0, Math.min(error.length(), 2000));
    }

    public boolean requestRecommendationRetry() {
        if (recommendationStatus != TestRecommendationStatus.FAILED) {
            return false;
        }
        recommendationStatus = TestRecommendationStatus.PENDING;
        recommendationError = null;
        return true;
    }
}
