package com.iread.backend.gaze.domain;

import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.training.domain.TrainingEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "gaze_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GazeSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private StudentTestEntity test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id")
    private TrainingEntity training;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private StoryEntity story;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private GazeContentType contentType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(columnDefinition = "json")
    private String data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GazeSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "calibration_status", nullable = false, length = 20)
    private GazeCalibrationStatus calibrationStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GazeSessionEntity(StudentEntity student, StudentTestEntity test, TrainingEntity training, StoryEntity story,
                             GazeContentType contentType, GazeCalibrationStatus calibrationStatus,
                             LocalDateTime startedAt) {
        this.student = student;
        this.test = test;
        this.training = training;
        this.story = story;
        this.contentType = contentType;
        this.calibrationStatus = calibrationStatus;
        this.startedAt = startedAt;
        this.status = GazeSessionStatus.RUNNING;
    }

    public void end(GazeSessionStatus status, LocalDateTime endedAt) {
        this.status = status;
        this.endedAt = endedAt;
    }

    public void fail(LocalDateTime endedAt) {
        end(GazeSessionStatus.FAILED, endedAt);
    }
}
