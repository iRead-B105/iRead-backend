package com.iread.backend.story.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "story_choices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_story_choices_story_line", columnNames = "story_line_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryChoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_line_id", nullable = false)
    private StoryLineEntity storyLine;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoryChoiceEntity(StoryLineEntity storyLine, String content) {
        this.storyLine = storyLine;
        this.content = content;
    }

    /** 선택은 기록됐지만 이어질 장면이 없어 다시 이어 쓸 때 답변을 갱신한다. */
    public void updateContent(String content) {
        this.content = content;
    }
}
