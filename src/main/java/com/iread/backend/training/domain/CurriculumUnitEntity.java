package com.iread.backend.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "curriculum_units")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurriculumUnitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;
}
