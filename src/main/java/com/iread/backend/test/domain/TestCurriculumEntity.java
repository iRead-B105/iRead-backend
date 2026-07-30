package com.iread.backend.test.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "test_curriculums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestCurriculumEntity {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public TestCurriculumEntity(Long id, StudentEntity student, LocalDateTime createdAt) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("검사 커리큘럼 ID는 1 이상이어야 합니다.");
        }
        this.id = id;
        this.student = student;
        this.status = TestStatus.NOT_STARTED.name();
        this.createdAt = createdAt;
    }

    public void start() {
        if (TestStatus.NOT_STARTED.name().equals(status)) {
            this.status = TestStatus.IN_PROGRESS.name();
        }
    }

    public void complete(LocalDateTime completedAt) {
        this.status = TestStatus.COMPLETED.name();
        this.completedAt = completedAt;
    }
}
