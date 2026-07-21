package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "student_study_progresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentStudyProgressEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "training_template_id", nullable = false)
    private TrainingTemplateEntity trainingTemplate;
    @Column(precision = 5, scale = 2)
    private BigDecimal achievement;
}
