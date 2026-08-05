package com.iread.backend.training.app.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.config.DemoTrainingProgressResetService;
import com.iread.backend.training.curriculum.CurriculumGenerationWorker;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoLearningCheatServiceTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final DailyCurriculumRepository curriculumRepository =
            mock(DailyCurriculumRepository.class);
    private final PersonalizedCurriculumPlanner curriculumPlanner =
            mock(PersonalizedCurriculumPlanner.class);
    private final CurriculumGenerationWorker generationWorker =
            mock(CurriculumGenerationWorker.class);
    private final DemoTrainingProgressResetService resetService =
            mock(DemoTrainingProgressResetService.class);
    private final TrainingRepository trainingRepository = mock(TrainingRepository.class);
    private final RealtimeEventPublisher realtimeEventPublisher = mock(RealtimeEventPublisher.class);
    private final DemoLearningClock learningClock = new DemoLearningClock(
            trainingRepository,
            Clock.fixed(
                    Instant.parse("2026-08-03T01:30:00Z"),
                    ZoneId.of("Asia/Seoul")
            )
    );

    private DemoLearningCheatService service;

    @BeforeEach
    void setUp() {
        service = new DemoLearningCheatService(
                studentRepository,
                curriculumRepository,
                curriculumPlanner,
                generationWorker,
                resetService,
                trainingRepository,
                realtimeEventPublisher,
                learningClock
        );
    }

    @Test
    void completesCurrentTrainingAndUnlocksNextTraining() {
        long teacherId = 1L;
        long studentId = 2103L;
        long trainingId = 181031L;
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity curriculum = mock(DailyCurriculumEntity.class);
        TrainingEntity current = mock(TrainingEntity.class);
        TrainingEntity next = mock(TrainingEntity.class);

        when(studentRepository.findByIdAndTeacherId(studentId, teacherId))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(trainingId, studentId))
                .thenReturn(Optional.of(current));
        when(current.isCompletable()).thenReturn(true);
        when(current.isCompleted()).thenReturn(false);
        when(current.getStartedAt()).thenReturn(null);
        when(current.getId()).thenReturn(trainingId);
        when(current.getSequenceNo()).thenReturn(1);
        when(current.getDailyCurriculum()).thenReturn(curriculum);
        when(current.getStatus()).thenReturn(TrainingStatus.COMPLETED);
        when(next.getId()).thenReturn(181032L);
        when(next.getSequenceNo()).thenReturn(2);
        when(next.getStatus()).thenReturn(
                TrainingStatus.NOT_READY,
                TrainingStatus.NOT_READY,
                TrainingStatus.NOT_STARTED
        );
        when(curriculum.getId()).thenReturn(180003L);
        when(curriculum.getStatus()).thenReturn(DailyCurriculumStatus.IN_PROGRESS);
        when(curriculum.getTrainings()).thenReturn(List.of(current, next));

        var result = service.advanceToNextTraining(teacherId, studentId, trainingId);

        verify(current).start(any(LocalDateTime.class));
        verify(current).complete(
                eq("{\"cheat\":true,\"action\":\"ADVANCE_TRAINING\"}"),
                eq(BigDecimal.valueOf(100)),
                any(LocalDateTime.class)
        );
        verify(next).markReady();
        verify(realtimeEventPublisher).publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.TRAINING,
                trainingId,
                "COMPLETED"
        );
        assertThat(result.currentDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(result.completedTrainingId()).isEqualTo(trainingId);
        assertThat(result.completedTrainingStatus()).isEqualTo("COMPLETED");
        assertThat(result.nextTrainingId()).isEqualTo(181032L);
        assertThat(result.nextTrainingStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    void doesNotAdvanceFromNotReadyTraining() {
        long teacherId = 1L;
        long studentId = 2103L;
        long trainingId = 181032L;
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity current = mock(TrainingEntity.class);

        when(studentRepository.findByIdAndTeacherId(studentId, teacherId))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(trainingId, studentId))
                .thenReturn(Optional.of(current));
        when(current.isCompletable()).thenReturn(false);
        when(current.isCompleted()).thenReturn(false);

        assertThatThrownBy(() ->
                service.advanceToNextTraining(teacherId, studentId, trainingId)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("준비되지 않은 훈련은 강제 완료할 수 없습니다.");

        verify(current, never()).complete(any(), any(), any());
    }

    @Test
    void advancingDayStoresTomorrowAndPublishesCurriculumChange() {
        long teacherId = 1L;
        long studentId = 2103L;
        StudentEntity student = mock(StudentEntity.class);
        DailyCurriculumEntity active = mock(DailyCurriculumEntity.class);
        DailyCurriculumEntity next = mock(DailyCurriculumEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);

        when(studentRepository.findByIdAndTeacherId(studentId, teacherId))
                .thenReturn(Optional.of(student));
        when(curriculumRepository.findByStudentIdAndStatus(
                studentId,
                DailyCurriculumStatus.IN_PROGRESS
        )).thenReturn(Optional.of(active));
        when(active.getTrainings()).thenReturn(List.of(training));
        when(training.isCompleted()).thenReturn(false);
        when(curriculumPlanner.createNextIfAbsent(student)).thenReturn(next);
        when(next.getId()).thenReturn(180004L);
        when(next.getStatus()).thenReturn(DailyCurriculumStatus.IN_PROGRESS);
        when(next.getTrainings()).thenReturn(List.of());

        var result = service.advanceToNextDay(teacherId, studentId);

        verify(training).complete(
                eq("{\"cheat\":true,\"action\":\"ADVANCE_DAY\"}"),
                eq(BigDecimal.valueOf(100)),
                eq(LocalDateTime.of(2026, 8, 4, 10, 30))
        );
        verify(generationWorker).generate(180004L);
        verify(realtimeEventPublisher).publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.CURRICULUM,
                180004L,
                "ADVANCED_TO_NEXT_DAY"
        );
        assertThat(result.currentDate()).isEqualTo(LocalDate.of(2026, 8, 4));
    }
}
