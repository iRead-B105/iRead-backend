package com.iread.backend.story.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "story_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public StoryTemplateEntity(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
