package com.iread.backend.story.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "story_scenes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorySceneEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scene_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StorySceneEntity(StoryEntity story, String imageUrl, Integer sequenceNo) {
        this.story = story;
        this.imageUrl = imageUrl;
        this.sequenceNo = sequenceNo;
    }

    public void updateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("스토리 장면 imageUrl은 필수입니다.");
        }
        this.imageUrl = imageUrl;
    }
}
