package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
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

    @Column(name = "not_started_student_id", insertable = false, updatable = false)
    private Long notStartedStudentId;

    @Column(name = "in_progress_student_id", insertable = false, updatable = false)
    private Long inProgressStudentId;

    @OneToMany(mappedBy = "dailyCurriculum", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    private List<TrainingEntity> trainings = new ArrayList<>();

    public DailyCurriculumEntity(StudentEntity student, List<TrainingTemplateEntity> templates) {
        this.student = Objects.requireNonNull(student, "student는 필수입니다.");
        replaceTrainings(templates);
    }

    public void replaceTrainings(List<TrainingTemplateEntity> templates) {
        trainings.clear();
        for (int index = 0; index < templates.size(); index++) {
            trainings.add(new TrainingEntity(this, templates.get(index), index + 1));
        }
        status = DailyCurriculumStatus.NOT_STARTED;
        completedAt = null;
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
