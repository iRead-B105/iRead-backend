package com.iread.backend.story.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "story_lines", uniqueConstraints = {
        @UniqueConstraint(name = "uk_story_lines_scene_sequence", columnNames = {"scene_id", "sequence_no"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private StoryLineEntity previousStoryLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private StorySceneEntity scene;

    @Column(name = "has_choices", nullable = false)
    private boolean requiresBranchInput;

    /**
     * 대사 본문과 형태소·G2P 분석 결과를 함께 담은 JSON 문자열.
     * {@code {"text": "...", "analysis": {...}}} 형태이며 analysis는 나중에 채워질 수 있다.
     */
    @Column(nullable = false, columnDefinition = "json")
    private String content;

    @Column(name = "branch_prompt", columnDefinition = "json")
    private String branchPrompt;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public StoryLineEntity(StoryLineEntity previousStoryLine, StorySceneEntity scene,
                           boolean requiresBranchInput, String content, Integer sequenceNo) {
        this(previousStoryLine, scene, requiresBranchInput, content, null, sequenceNo);
    }

    public StoryLineEntity(StoryLineEntity previousStoryLine, StorySceneEntity scene,
                           boolean requiresBranchInput, String content, String branchPrompt, Integer sequenceNo) {
        this.previousStoryLine = previousStoryLine;
        this.scene = scene;
        this.requiresBranchInput = requiresBranchInput;
        this.content = content;
        this.branchPrompt = branchPrompt;
        this.sequenceNo = sequenceNo;
    }

    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("스토리 대사 content는 필수입니다.");
        }
        this.content = content;
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public StoryEntity getStory() {
        return scene.getStory();
    }

    public String getImageUrl() {
        return scene.getImageUrl();
    }
}
