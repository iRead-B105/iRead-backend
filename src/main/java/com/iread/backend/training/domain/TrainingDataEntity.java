package com.iread.backend.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "training_datas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingDataEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_id", nullable = false, unique = true)
    private TrainingEntity training;

    @Column(name = "generated_data", nullable = false, columnDefinition = "json")
    private String generatedData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TrainingDataEntity(TrainingEntity training, String generatedData) {
        this.training = training;
        this.generatedData = generatedData;
    }

    public void updateGeneratedData(String generatedData) { this.generatedData = generatedData; }
}
