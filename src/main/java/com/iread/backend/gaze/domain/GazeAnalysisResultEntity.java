package com.iread.backend.gaze.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "gaze_analysis_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GazeAnalysisResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gaze_session_id", nullable = false, unique = true)
    private GazeSessionEntity gazeSession;

    @Column(name = "total_visited_duration", nullable = false)
    private Integer totalVisitedDuration;

    @Column(name = "total_visited_count", nullable = false)
    private Integer totalVisitedCount;

    @Column(name = "reverse_read_count", nullable = false)
    private Integer reverseReadCount;

    @Column(name = "avg_visited_duration")
    private Integer avgVisitedDuration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GazeAnalysisResultEntity(GazeSessionEntity gazeSession, Integer totalVisitedDuration,
                                    Integer totalVisitedCount, Integer reverseReadCount,
                                    Integer avgVisitedDuration) {
        this.gazeSession = gazeSession;
        this.totalVisitedDuration = totalVisitedDuration;
        this.totalVisitedCount = totalVisitedCount;
        this.reverseReadCount = reverseReadCount;
        this.avgVisitedDuration = avgVisitedDuration;
    }
}
