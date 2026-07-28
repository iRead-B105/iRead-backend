package com.iread.backend.training.domain;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Convert(converter = AccuracyIntegerConverter.class)
    @Column(columnDefinition = "int")
    private BigDecimal accuracy;

    @OneToMany(mappedBy = "training", cascade = CascadeType.REMOVE)
    private List<WordAttemptLogEntity> wordAttemptLogs = new ArrayList<>();

    TrainingEntity(DailyCurriculumEntity dailyCurriculum, TrainingTemplateEntity trainingTemplate, Integer sequenceNo) {
        this.dailyCurriculum = dailyCurriculum;
        this.trainingTemplate = trainingTemplate;
        this.sequenceNo = sequenceNo;
    }

    public boolean isCompleted() { return status == TrainingStatus.COMPLETED; }
    public boolean isEditable() { return status == TrainingStatus.NOT_READY; }
    public boolean isCompletable() {
        return status == TrainingStatus.NOT_STARTED || status == TrainingStatus.IN_PROGRESS;
    }
    public void markNotReady() { status = TrainingStatus.NOT_READY; }
    public void markReady() { status = TrainingStatus.NOT_STARTED; }

    public void start(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        this.status = TrainingStatus.IN_PROGRESS;
    }

    public void reset() {
        if (status == TrainingStatus.COMPLETED) {
            throw new ConflictException("완료된 훈련은 초기화할 수 없습니다.");
        }
        startedAt = null;
        finishedAt = null;
        result = null;
        accuracy = null;
        status = TrainingStatus.NOT_STARTED;
    }

    public void recordProgressResult(String result) {
        if (status != TrainingStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 훈련에만 시도 결과를 연결할 수 있습니다.");
        }
        this.result = result;
    }

    public void complete(String result, BigDecimal accuracy, LocalDateTime finishedAt) {
        this.result = result;
        this.accuracy = accuracy;
        this.finishedAt = finishedAt;
        this.status = TrainingStatus.COMPLETED;
        dailyCurriculum.refreshCompletion(finishedAt);
    }
}
