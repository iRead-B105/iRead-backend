package com.iread.backend.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "training_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingTemplateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_unit_id", nullable = false)
    private CurriculumUnitEntity curriculumUnit;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String form;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;
}
