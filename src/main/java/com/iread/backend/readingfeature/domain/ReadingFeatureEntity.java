package com.iread.backend.readingfeature.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reading_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingFeatureEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_feature_id")
    private ReadingFeatureEntity parentFeature;

    @Column(name = "feature_code", nullable = false, length = 150)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 150)
    private String featureName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReadingFeatureCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReadingFeatureScope scope;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ReadingFeatureEntity(Long id, ReadingFeatureEntity parentFeature, String featureCode,
                                String featureName, ReadingFeatureCategory category,
                                ReadingFeatureScope scope) {
        this.id = id;
        this.parentFeature = parentFeature;
        this.featureCode = featureCode;
        this.featureName = featureName;
        this.category = category;
        this.scope = scope;
    }
}
