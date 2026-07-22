package com.iread.backend.story.domain;

import com.iread.backend.global.domain.ImageEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "story_lines", uniqueConstraints = {
        @UniqueConstraint(name = "uk_story_lines_story_sequence", columnNames = {"story_id", "sequence_no"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_story_line_id")
    private StoryLineEntity previousStoryLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private ImageEntity image;

    @Column(name = "has_choices", nullable = false)
    private boolean hasChoices;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public StoryLineEntity(StoryLineEntity previousStoryLine, StoryEntity story, ImageEntity image,
                           boolean hasChoices, String content, Integer sequenceNo) {
        this.previousStoryLine = previousStoryLine;
        this.story = story;
        this.image = image;
        this.hasChoices = hasChoices;
        this.content = content;
        this.sequenceNo = sequenceNo;
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
