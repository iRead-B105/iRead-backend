package com.iread.backend.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "trainings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_template_id", nullable = false)
    private TrainingTemplateEntity trainingTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_curriculum_id", nullable = false)
    private DailyCurriculumEntity dailyCurriculum;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingStatus status = TrainingStatus.NOT_READY;

    @Column(columnDefinition = "json")
    private String result;

    @Column(precision = 5, scale = 2)
    private BigDecimal accuracy;

    TrainingEntity(DailyCurriculumEntity dailyCurriculum, TrainingTemplateEntity trainingTemplate, Integer sequenceNo) {
        this.dailyCurriculum = dailyCurriculum;
        this.trainingTemplate = trainingTemplate;
        this.sequenceNo = sequenceNo;
    }

    public boolean isCompleted() { return status == TrainingStatus.COMPLETED; }
    public boolean isEditable() { return status == TrainingStatus.NOT_READY || status == TrainingStatus.NOT_STARTED; }
    public boolean isCompletable() {
        return status == TrainingStatus.NOT_STARTED || status == TrainingStatus.IN_PROGRESS;
    }
    public void markNotReady() { status = TrainingStatus.NOT_READY; }
    public void markReady() { status = TrainingStatus.NOT_STARTED; }

    public void complete(String result, BigDecimal accuracy, LocalDateTime finishedAt) {
        this.result = result;
        this.accuracy = accuracy;
        this.finishedAt = finishedAt;
        this.status = TrainingStatus.COMPLETED;
        dailyCurriculum.refreshCompletion(finishedAt);
    }
}
