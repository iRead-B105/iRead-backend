package com.iread.backend.test.domain;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentTestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_curriculum_id", nullable = false)
    private TestCurriculumEntity testCurriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_template_id", nullable = false)
    private TrainingTemplateEntity trainingTemplate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestStatus status;

    @Column(columnDefinition = "json")
    private String result;

    @Column(precision = 10)
    private BigDecimal accuracy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    public StudentTestEntity(
            TestCurriculumEntity testCurriculum,
            TrainingTemplateEntity trainingTemplate,
            Integer sequenceNo
    ) {
        if (sequenceNo == null || sequenceNo < 1) {
            throw new IllegalArgumentException("검사 순서는 1 이상이어야 합니다.");
        }
        this.testCurriculum = testCurriculum;
        this.trainingTemplate = trainingTemplate;
        this.status = TestStatus.NOT_STARTED;
        this.sequenceNo = sequenceNo;
    }

    public StudentEntity getStudent() {
        return testCurriculum.getStudent();
    }

    public void start(LocalDateTime startedAt) {
        if (status != TestStatus.NOT_STARTED) {
            throw new ConflictException("시작 가능한 검사가 아닙니다.");
        }
        this.status = TestStatus.IN_PROGRESS;
        this.startedAt = startedAt;
    }

    public void updateResult(String result) {
        if (status != TestStatus.IN_PROGRESS) {
            throw new ConflictException("진행 중인 검사가 아닙니다.");
        }
        this.result = result;
    }

    public void complete(String result, BigDecimal accuracy, LocalDateTime finishedAt) {
        if (status != TestStatus.IN_PROGRESS) {
            throw new ConflictException("완료 가능한 검사가 아닙니다.");
        }
        this.status = TestStatus.COMPLETED;
        this.result = result;
        this.accuracy = accuracy;
        this.finishedAt = finishedAt;
    }

    public void reset() {
        if (status == TestStatus.COMPLETED) {
            throw new ConflictException("완료된 검사는 초기화할 수 없습니다.");
        }
        this.status = TestStatus.NOT_STARTED;
        this.result = null;
        this.accuracy = null;
        this.startedAt = null;
        this.finishedAt = null;
    }
}
