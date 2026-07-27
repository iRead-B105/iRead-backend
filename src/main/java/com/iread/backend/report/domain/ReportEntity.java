package com.iread.backend.report.domain;

import com.iread.backend.student.domain.StudentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "snapshot_data", columnDefinition = "json")
    private String snapshotData;

    @Column(name = "teacher_memo", columnDefinition = "text")
    private String teacherMemo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReportEntity(StudentEntity student, LocalDate startDate, LocalDate endDate,
                        String snapshotData, String teacherMemo) {
        this.student = student;
        this.startDate = startDate.atStartOfDay();
        this.endDate = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        this.snapshotData = snapshotData;
        this.teacherMemo = teacherMemo;
    }

    public LocalDate getStartDate() {
        return startDate.toLocalDate();
    }

    public LocalDate getEndDate() {
        return endDate.toLocalDate();
    }

    public void updateTeacherMemo(String teacherMemo) {
        this.teacherMemo = teacherMemo == null || teacherMemo.isBlank() ? null : teacherMemo.trim();
    }

    public void updateSnapshotData(String snapshotData) {
        this.snapshotData = snapshotData;
    }
}
