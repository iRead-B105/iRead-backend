package com.iread.backend.report.domain;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.WordEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "student_word_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentWordStatEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntity word;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;
}
