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
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "snapshot_data", nullable = false, columnDefinition = "json")
    private String snapshotData;

    @Column(name = "teacher_memo", columnDefinition = "text")
    private String teacherMemo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReportEntity(StudentEntity student, LocalDate startDate, LocalDate endDate,
                        String snapshotData, String teacherMemo) {
        this.student = student;
        this.startDate = startDate;
        this.endDate = endDate;
        this.snapshotData = snapshotData;
        this.teacherMemo = teacherMemo;
    }
}
