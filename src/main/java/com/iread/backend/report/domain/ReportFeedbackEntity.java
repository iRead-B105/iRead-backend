package com.iread.backend.report.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "report_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_share_id", nullable = false)
    private ReportShareEntity reportShare;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public ReportFeedbackEntity(ReportShareEntity reportShare, String content) {
        this.reportShare = reportShare;
        this.content = content;
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }
}
