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
}
