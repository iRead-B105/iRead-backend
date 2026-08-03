package com.iread.backend.story.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "story_page_edit_audits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryPageEditAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_line_id", nullable = false)
    private Long storyLineId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "edit_type", nullable = false, length = 30)
    private String editType;

    @Column(name = "before_value", columnDefinition = "json")
    private String beforeValue;

    @Column(name = "after_value", nullable = false, columnDefinition = "json")
    private String afterValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoryPageEditAuditEntity(Long storyLineId, Long teacherId, String editType,
                                    String beforeValue, String afterValue) {
        this.storyLineId = storyLineId;
        this.teacherId = teacherId;
        this.editType = editType;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }
}
