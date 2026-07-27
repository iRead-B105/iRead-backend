package com.iread.backend.test.domain;

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

    public StudentEntity getStudent() {
        return testCurriculum.getStudent();
    }
}
