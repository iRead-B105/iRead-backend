package com.iread.backend.story.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "stories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_template_id", nullable = false)
    private StoryTemplateEntity storyTemplate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StoryStatus status = StoryStatus.IN_PROGRESS;

    public StoryEntity(StudentEntity student, StoryTemplateEntity storyTemplate) {
        this.student = student;
        this.storyTemplate = storyTemplate;
    }

    public boolean isInProgress() {
        return status == StoryStatus.IN_PROGRESS;
    }

    public void complete() {
        status = StoryStatus.COMPLETED;
    }
}
