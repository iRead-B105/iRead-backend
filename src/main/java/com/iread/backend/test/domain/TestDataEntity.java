package com.iread.backend.test.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "test_datas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestDataEntity {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private StudentTestEntity test;

    @Column(name = "generated_data", columnDefinition = "json")
    private String generatedData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TestDataEntity(
            Long id,
            StudentTestEntity test,
            String generatedData,
            LocalDateTime createdAt
    ) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("검사 데이터 ID는 1 이상이어야 합니다.");
        }
        this.id = id;
        this.test = test;
        this.generatedData = generatedData;
        this.createdAt = createdAt;
    }

    public void updateGeneratedData(String generatedData) {
        this.generatedData = generatedData;
    }
}
