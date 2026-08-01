package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "daily_curriculums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCurriculumEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyCurriculumStatus status = DailyCurriculumStatus.NOT_STARTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_test_curriculum_id", unique = true)
    private TestCurriculumEntity sourceTestCurriculum;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "review_status",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'NOT_REQUIRED'"
    )
    private CurriculumReviewStatus reviewStatus = CurriculumReviewStatus.NOT_REQUIRED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_teacher_id")
    private TeacherEntity reviewedByTeacher;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "not_started_student_id", insertable = false, updatable = false)
    private Long notStartedStudentId;

    @Column(name = "in_progress_student_id", insertable = false, updatable = false)
    private Long inProgressStudentId;

    @OneToMany(mappedBy = "dailyCurriculum", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    private List<TrainingEntity> trainings = new ArrayList<>();

    public DailyCurriculumEntity(StudentEntity student, List<TrainingTemplateEntity> templates) {
        this(student, templates, null);
    }

    public DailyCurriculumEntity(
            StudentEntity student,
            List<TrainingTemplateEntity> templates,
            TestCurriculumEntity sourceTestCurriculum
    ) {
        this.student = Objects.requireNonNull(student, "student는 필수입니다.");
        this.sourceTestCurriculum = sourceTestCurriculum;
        replaceTrainings(templates);
        this.reviewStatus = sourceTestCurriculum == null
                ? CurriculumReviewStatus.NOT_REQUIRED
                : CurriculumReviewStatus.GENERATION_PENDING;
    }

    public void replaceTrainings(List<TrainingTemplateEntity> templates) {
        trainings.clear();
        for (int index = 0; index < templates.size(); index++) {
            trainings.add(new TrainingEntity(this, templates.get(index), index + 1));
        }
        status = DailyCurriculumStatus.NOT_STARTED;
        completedAt = null;
        if (sourceTestCurriculum != null) {
            markRegenerationRequired();
        }
    }

    public boolean isRecommendedFromTest() {
        return sourceTestCurriculum != null;
    }

    public boolean isAvailableToStudent() {
        return !isRecommendedFromTest()
                || reviewStatus == CurriculumReviewStatus.REVIEW_COMPLETED;
    }

    public void markRegenerationRequired() {
        if (!isRecommendedFromTest()) {
            return;
        }
        reviewStatus = CurriculumReviewStatus.REGENERATION_REQUIRED;
        clearReview();
    }

    public void refreshReviewRequirement() {
        if (!isRecommendedFromTest()
                || reviewStatus == CurriculumReviewStatus.REVIEW_COMPLETED) {
            return;
        }
        if (trainings.size() == 5 && trainings.stream().allMatch(training ->
                training.getStatus() == TrainingStatus.NOT_STARTED)) {
            reviewStatus = CurriculumReviewStatus.REVIEW_REQUIRED;
            clearReview();
        }
    }

    public void markContentChanged() {
        if (!isRecommendedFromTest()) {
            return;
        }
        clearReview();
        reviewStatus = trainings.size() == 5 && trainings.stream().allMatch(training ->
                training.getStatus() == TrainingStatus.NOT_STARTED)
                ? CurriculumReviewStatus.REVIEW_REQUIRED
                : CurriculumReviewStatus.REGENERATION_REQUIRED;
    }

    public void completeReview(TeacherEntity teacher, LocalDateTime reviewedAt) {
        if (!isRecommendedFromTest()) {
            throw new IllegalStateException("Only test-recommended curricula require review.");
        }
        if (reviewStatus == CurriculumReviewStatus.REVIEW_COMPLETED) {
            return;
        }
        if (reviewStatus != CurriculumReviewStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("Curriculum content is not ready for final review.");
        }
        reviewedByTeacher = Objects.requireNonNull(teacher, "teacher is required");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt is required");
        reviewStatus = CurriculumReviewStatus.REVIEW_COMPLETED;
    }

    private void clearReview() {
        reviewedByTeacher = null;
        reviewedAt = null;
    }

    public void refreshCompletion(LocalDateTime now) {
        if (!trainings.isEmpty() && trainings.stream().allMatch(TrainingEntity::isCompleted)) {
            status = DailyCurriculumStatus.COMPLETED;
            if (completedAt == null) completedAt = now;
        }
    }

    void markInProgress() {
        if (status == DailyCurriculumStatus.NOT_STARTED) {
            status = DailyCurriculumStatus.IN_PROGRESS;
        }
    }
}
