package com.iread.backend.training.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "words")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String content;
    @Column
    private Integer length;

    public WordEntity(String content) {
        this.content = content;
        this.length = content.length();
    }
}
