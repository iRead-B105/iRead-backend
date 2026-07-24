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
        @UniqueConstraint(name = "uk_story_lines_story_sequence", columnNames = {"story_id", "sequence_no"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_line_id")
    private StoryLineEntity previousStoryLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "requires_branch_input", nullable = false)
    private boolean requiresBranchInput;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public StoryLineEntity(StoryLineEntity previousStoryLine, StoryEntity story, String imageUrl,
                           boolean requiresBranchInput, String content, Integer sequenceNo) {
        this.previousStoryLine = previousStoryLine;
        this.story = story;
        this.imageUrl = imageUrl;
        this.requiresBranchInput = requiresBranchInput;
        this.content = content;
        this.sequenceNo = sequenceNo;
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
