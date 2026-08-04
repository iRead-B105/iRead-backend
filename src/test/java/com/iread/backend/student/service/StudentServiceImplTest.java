package com.iread.backend.student.service;

import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.student.domain.Gender;
import com.iread.backend.student.domain.LearningEventType;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.repository.StudentDeletionRepository;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentDeletionRepository studentDeletionRepository;
    @Mock StudentDeletionResourceCleaner studentDeletionResourceCleaner;
    @Mock TeacherRepository teacherRepository;
    @Mock FileStorage fileStorage;
    @Mock RealtimeEventPublisher realtimeEventPublisher;

    private StudentServiceImpl studentService;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(
                studentRepository,
                studentDeletionRepository,
                studentDeletionResourceCleaner,
                teacherRepository,
                fileStorage,
                realtimeEventPublisher,
                JsonMapper.builder().build(),
                new ReadingMetricAggregationService(studentRepository)
        );
        teacher = new TeacherEntity(
                "teacher@test.com", "encoded-password", "교사", "기관",
                com.iread.backend.teacher.domain.Gender.FEMALE, null
        );
        ReflectionTestUtils.setField(teacher, "id", 1L);
    }

    @Test
    void deletesOwnedStudentDependenciesBeforeStudentAndSchedulesResourceCleanup() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name("학생")
                .imageUrl("/uploads/images/student.png")
                .build();
        ReflectionTestUtils.setField(student, "id", 10L);
        List<String> gazeDataUrls = List.of("/gaze/10/gaze-1-00000000-0000-0000-0000-000000000000.json");
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentDeletionRepository.findGazeDataUrls(10L)).thenReturn(gazeDataUrls);

        studentService.deleteStudent(1L, 10L);

        var ordered = inOrder(
                studentDeletionRepository,
                studentRepository,
                studentDeletionResourceCleaner,
                realtimeEventPublisher
        );
        ordered.verify(studentDeletionRepository).findGazeDataUrls(10L);
        ordered.verify(studentDeletionRepository).deleteDependencies(10L);
        ordered.verify(studentRepository).delete(student);
        ordered.verify(studentDeletionResourceCleaner).cleanAfterCommit(
                10L,
                "/uploads/images/student.png",
                gazeDataUrls
        );
        ordered.verify(realtimeEventPublisher).publishAfterCommit(
                1L,
                10L,
                com.iread.backend.realtime.RealtimeResource.STUDENT,
                10L,
                "DELETED"
        );
    }

    @Test
    void doesNotDeleteStudentOrScheduleSideEffectsWhenDependencyDeletionFails() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name("학생")
                .imageUrl("/uploads/images/student.png")
                .build();
        ReflectionTestUtils.setField(student, "id", 10L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentDeletionRepository.findGazeDataUrls(10L)).thenReturn(List.of());
        doThrow(new RuntimeException("delete failed"))
                .when(studentDeletionRepository).deleteDependencies(10L);

        assertThatThrownBy(() -> studentService.deleteStudent(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("delete failed");

        verify(studentRepository, never()).delete(any(StudentEntity.class));
        verify(studentDeletionResourceCleaner, never())
                .cleanAfterCommit(any(), any(), any());
        verify(realtimeEventPublisher, never())
                .publishAfterCommit(any(), any(), any(), any(), any());
    }

    @Test
    void savesUploadedImageUrlDirectlyOnStudent() {
        StudentRequest request = request(null);
        MockMultipartFile image = imageFile("profile.png");
        StoredFile stored = new StoredFile(
                "profile.png", "stored-profile.png", image.getSize(),
                "/uploads/images/stored-profile.png"
        );
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(fileStorage.store(image)).thenReturn(stored);

        studentService.createStudent(1L, request, image);

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl())
                .isEqualTo("/uploads/images/stored-profile.png");
    }

    @Test
    void usesBoyProfileImageByDefaultWhenCreatingBoy() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        studentService.createStudent(1L, request(null, Gender.Boy));

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl())
                .isEqualTo("/images/student-profile-boy.png");
    }

    @Test
    void usesGirlProfileImageByDefaultWhenCreatingGirl() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        studentService.createStudent(1L, request(null, Gender.Girl));

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl())
                .isEqualTo("/images/student-profile-girl.png");
    }

    @Test
    void preservesExplicitProfileImageWhenCreatingStudent() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        studentService.createStudent(1L, request("/images/custom-profile.png", Gender.Girl));

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getImageUrl())
                .isEqualTo("/images/custom-profile.png");
    }

    @Test
    void deletesUploadedFileWhenStudentSaveFails() {
        StudentRequest request = request(null);
        MockMultipartFile image = imageFile("profile.png");
        StoredFile stored = new StoredFile(
                "profile.png", "stored-profile.png", image.getSize(),
                "/uploads/images/stored-profile.png"
        );
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(fileStorage.store(image)).thenReturn(stored);
        doThrow(new RuntimeException("save failed")).when(studentRepository).save(any(StudentEntity.class));

        assertThatThrownBy(() -> studentService.createStudent(1L, request, image))
                .isInstanceOf(RuntimeException.class);
        verify(fileStorage).delete("stored-profile.png");
    }

    @Test
    void replacesImageUrlAndDeletesOldLocalFile() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생")
                .birthday(LocalDate.of(2016, 3, 10)).gender(Gender.Boy)
                .imageUrl("/uploads/images/old-stored.png").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        MockMultipartFile image = imageFile("new.png");
        StoredFile stored = new StoredFile(
                "new.png", "new-stored.png", image.getSize(),
                "/uploads/images/new-stored.png"
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(fileStorage.store(image)).thenReturn(stored);

        studentService.updateStudent(1L, 10L, request(null), image);

        assertThat(student.getImageUrl()).isEqualTo("/uploads/images/new-stored.png");
        verify(fileStorage).delete("old-stored.png");
    }

    @Test
    void updatesTeacherMemoForOwnedStudent() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        studentService.updateTeacherMemo(1L, 10L, "  반복 읽기 필요  ");

        assertThat(student.getTeacherMemo()).isEqualTo("반복 읽기 필요");
    }

    @Test
    void clearsTeacherMemoWithBlankText() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        student.updateTeacherMemo("기존 메모");
        ReflectionTestUtils.setField(student, "id", 10L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        studentService.updateTeacherMemo(1L, 10L, " ");

        assertThat(student.getTeacherMemo()).isNull();
    }

    @Test
    void filtersAndPagesOwnedStudentsByFrontendListContract() {
        StudentEntity included = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentEntity excluded = student(11L, "서연", LocalDate.now().minusYears(8));
        StudentRepository.StudentLearningSummaryProjection includedSummary =
                learningSummary(10L, LocalDate.now().minusDays(2));
        StudentRepository.StudentLearningSummaryProjection excludedSummary =
                learningSummary(11L, LocalDate.now().minusDays(20));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.findAllByTeacherIdOrderByIdAsc(1L))
                .thenReturn(List.of(included, excluded));
        when(studentRepository.findLearningSummaries(1L))
                .thenReturn(List.of(includedSummary, excludedSummary));

        var result = studentService.getStudents(
                1L,
                "민",
                10,
                7,
                0,
                10
        );

        assertThat(result.students()).extracting("studentId").containsExactly(10L);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void searchesSchoolAndReturnsAnEmptyOutOfRangePage() {
        StudentEntity first = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentEntity second = student(11L, "서연", LocalDate.now().minusYears(10));
        ReflectionTestUtils.setField(first, "school", "새봄초");
        ReflectionTestUtils.setField(second, "school", "새봄초");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.findAllByTeacherIdOrderByIdAsc(1L))
                .thenReturn(List.of(first, second));
        when(studentRepository.findLearningSummaries(1L)).thenReturn(List.of());

        var result = studentService.getStudents(
                1L, "새봄", null, null, 2, 1
        );

        assertThat(result.students()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsUnsupportedStudentListQueryValues() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> studentService.getStudents(
                1L, null, 5, null, 0, 10
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("age");
        assertThatThrownBy(() -> studentService.getStudents(
                1L, null, null, 14, 0, 10
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recentDays");
        assertThatThrownBy(() -> studentService.getStudents(
                1L, null, null, null, -1, 10
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page");
    }

    @Test
    void returnsStudentDashboardSummaryForCurrentTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.countByTeacherId(1L)).thenReturn(7L);
        when(studentRepository.countScheduledToday(1L)).thenReturn(3L);

        var result = studentService.getStudentSummary(1L);

        assertThat(result.totalStudents()).isEqualTo(7L);
        assertThat(result.scheduledTodayCount()).isEqualTo(3L);
    }

    @Test
    void appliesAttentionRulesToLearningSummary() {
        StudentEntity student = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentRepository.LearningOverviewProjection overview =
                org.mockito.Mockito.mock(StudentRepository.LearningOverviewProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findLearningOverview(10L)).thenReturn(overview);
        when(overview.getCurrentStage()).thenReturn("문장 읽기");
        when(overview.getLastLearningAt()).thenReturn(LocalDateTime.now().minusDays(15));
        when(overview.getRecentCompletedCount()).thenReturn(3L);
        when(overview.getRecentAverageAccuracy()).thenReturn(new BigDecimal("69.99"));
        when(overview.getRecentGazeFailureCount()).thenReturn(1L);

        var result = studentService.getLearningSummary(1L, 10L);

        assertThat(result.currentStage()).isEqualTo("문장 읽기");
        assertThat(result.attentionRequiredCount()).isEqualTo(3);
        assertThat(result.attentionReasons())
                .containsExactly("LOW_ACCURACY", "GAZE_ANALYSIS_FAILED", "INACTIVE");
    }

    @Test
    void returnsLearningEventWithDeterministicRecommendation() {
        StudentEntity student = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentRepository.LearningEventProjection event =
                org.mockito.Mockito.mock(StudentRepository.LearningEventProjection.class);
        StudentRepository.LearningOverviewProjection overview =
                org.mockito.Mockito.mock(StudentRepository.LearningOverviewProjection.class);
        StudentRepository.TrainingRecommendationProjection recommendation =
                org.mockito.Mockito.mock(StudentRepository.TrainingRecommendationProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findLearningEvent(10L, 100L)).thenReturn(Optional.of(event));
        when(studentRepository.findLearningOverview(10L)).thenReturn(overview);
        when(studentRepository.findTrainingRecommendation(10L))
                .thenReturn(Optional.of(recommendation));
        when(event.getEventId()).thenReturn(100L);
        when(event.getOccurredAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(event.getAccuracy()).thenReturn(new BigDecimal("82.50"));
        when(event.getRetryCount()).thenReturn(2L);
        when(event.getProblemSegments()).thenReturn("사과|||바나나");
        when(overview.getLastLearningAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(overview.getRecentCompletedCount()).thenReturn(1L);
        when(overview.getRecentGazeFailureCount()).thenReturn(0L);
        when(recommendation.getTrainingTemplateId()).thenReturn(30L);
        when(recommendation.getCurriculumUnitId()).thenReturn(3L);
        when(recommendation.getCurriculumUnitName()).thenReturn("문장 읽기");
        when(recommendation.getAverageAccuracy()).thenReturn(new BigDecimal("65.00"));

        var result = studentService.getLearningEvent(
                1L, 10L, LearningEventType.TRAINING, 100L);

        assertThat(result.eventType()).isEqualTo("training");
        assertThat(result.problemSegments()).containsExactly("사과", "바나나");
        assertThat(result.recommendedTrainingTemplateId()).isEqualTo(30L);
        assertThat(result.recommendedMinutes()).isEqualTo(10);
        assertThat(result.recommendedRepeatCount()).isEqualTo(2);
    }

    @Test
    void returnsRecentLearningEventsWithTypeQualifiedSourceIds() {
        StudentEntity student = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentRepository.RecentLearningEventProjection training =
                org.mockito.Mockito.mock(StudentRepository.RecentLearningEventProjection.class);
        StudentRepository.RecentLearningEventProjection test =
                org.mockito.Mockito.mock(StudentRepository.RecentLearningEventProjection.class);
        StudentRepository.LearningOverviewProjection overview =
                org.mockito.Mockito.mock(StudentRepository.LearningOverviewProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findRecentLearningEvents(10L, 3))
                .thenReturn(List.of(training, test));
        when(studentRepository.findLearningOverview(10L)).thenReturn(overview);
        when(training.getEventId()).thenReturn(100L);
        when(training.getEventType()).thenReturn("training");
        when(training.getOccurredAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 16, 0));
        when(training.getAccuracy()).thenReturn(new BigDecimal("82.50"));
        when(test.getEventId()).thenReturn(100L);
        when(test.getEventType()).thenReturn("test");
        when(test.getOccurredAt()).thenReturn(LocalDateTime.of(2026, 7, 27, 16, 0));
        when(test.getAccuracy()).thenReturn(new BigDecimal("75.00"));
        when(overview.getRecentCompletedCount()).thenReturn(3L);
        when(overview.getRecentAverageAccuracy()).thenReturn(new BigDecimal("69.00"));
        when(overview.getLastLearningAt()).thenReturn(LocalDateTime.now().minusDays(1));

        var result = studentService.getRecentLearningEvents(1L, 10L, 3);

        assertThat(result.events()).extracting("eventType")
                .containsExactly("training", "test");
        assertThat(result.events()).extracting("sourceId")
                .containsExactly(100L, 100L);
        assertThat(result.events()).allSatisfy(event -> {
            assertThat(event.attentionRequired()).isTrue();
            assertThat(event.attentionReasons()).contains("LOW_ACCURACY");
        });
    }

    @Test
    void rejectsRecentLearningEventLimitOutsideContractRange() {
        StudentEntity student = student(10L, "민준", LocalDate.now().minusYears(10));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.getRecentLearningEvents(1L, 10L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void dispatchesLearningEventLookupByEventType() {
        StudentEntity student = student(10L, "민준", LocalDate.now().minusYears(10));
        StudentRepository.LearningEventProjection event =
                org.mockito.Mockito.mock(StudentRepository.LearningEventProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findTestLearningEvent(10L, 100L)).thenReturn(Optional.of(event));
        when(studentRepository.findLearningEvent(10L, 100L)).thenReturn(Optional.of(event));
        when(studentRepository.findStoryLearningEvent(10L, 100L)).thenReturn(Optional.of(event));
        when(studentRepository.findGazeLearningEvent(10L, 100L)).thenReturn(Optional.of(event));
        when(studentRepository.findTrainingRecommendation(10L)).thenReturn(Optional.empty());
        when(event.getEventId()).thenReturn(100L);
        when(event.getOccurredAt()).thenReturn(LocalDateTime.of(2026, 7, 27, 12, 0));

        assertThat(studentService.getLearningEvent(
                1L, 10L, LearningEventType.TEST, 100L).eventType()).isEqualTo("test");
        assertThat(studentService.getLearningEvent(
                1L, 10L, LearningEventType.TRAINING, 100L).eventType()).isEqualTo("training");
        assertThat(studentService.getLearningEvent(
                1L, 10L, LearningEventType.STORY, 100L).eventType()).isEqualTo("story");
        assertThat(studentService.getLearningEvent(
                1L, 10L, LearningEventType.GAZE, 100L).eventType()).isEqualTo("gaze");
        assertThatThrownBy(() -> studentService.getLearningEvent(
                1L, 10L, null, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType은 필수입니다.");
    }

    @Test
    void aggregatesFinalAttemptAccuracyRecordsByDate() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        StudentRepository.AccuracyRecordProjection row =
                org.mockito.Mockito.mock(StudentRepository.AccuracyRecordProjection.class);
        LocalDate learningDate = LocalDate.now().minusDays(1);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findAccuracyRecords(any(), any(), any())).thenReturn(List.of(row));
        when(row.getSourceId()).thenReturn(100L);
        when(row.getMeasuredAt()).thenReturn(learningDate.atTime(15, 30));
        when(row.getCorrectAttemptCount()).thenReturn(8L);
        when(row.getAttemptCount()).thenReturn(10L);

        var result = studentService.getAccuracyTrend(1L, 10L);

        assertThat(result.unit()).isEqualTo("PERCENT");
        assertThat(result.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(result.dailyAccuracy()).hasSize(1);
        assertThat(result.dailyAccuracy().getFirst().date()).isEqualTo(learningDate);
        assertThat(result.dailyAccuracy().getFirst().correctAttemptCount()).isEqualTo(8L);
        assertThat(result.dailyAccuracy().getFirst().attemptCount()).isEqualTo(10L);
        assertThat(result.dailyAccuracy().getFirst().accuracyRate()).isEqualByComparingTo("80.00");
    }

    @Test
    void mapsTrainingResultQuestionsToHistoryResponse() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        StudentRepository.TrainingHistoryProjection row =
                org.mockito.Mockito.mock(StudentRepository.TrainingHistoryProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(studentRepository.findTrainingHistory(10L, from, to)).thenReturn(List.of(row));
        when(row.getTrainingId()).thenReturn(100L);
        when(row.getLearningDate()).thenReturn(LocalDate.of(2026, 7, 23));
        when(row.getLearningType()).thenReturn("소리 듣고 말하기");
        when(row.getLearningCategory()).thenReturn("낱말 읽기");
        when(row.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 23, 15, 30));
        when(row.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 23, 15, 30));
        when(row.getAchievement()).thenReturn(new BigDecimal("800"));
        when(row.getResult()).thenReturn("""
                {
                  "questions": [{
                    "questionNumber": 1,
                    "question": "사과를 읽어 보세요.",
                    "isCorrect": false,
                    "selectedAnswer": "사가",
                    "correctAnswer": "사과"
                  }]
                }
                """);

        var result = studentService.getTrainingHistory(1L, 10L, from, to);

        assertThat(result.getFirst().trainingId()).isEqualTo(100L);
        assertThat(result.getFirst().learningCategory()).isEqualTo("낱말 읽기");
        assertThat(result.getFirst().accuracyRate()).isEqualByComparingTo("80.00");
        assertThat(result.getFirst().questions()).hasSize(1);
        assertThat(result.getFirst().questions().getFirst().question()).isEqualTo("사과를 읽어 보세요.");
        assertThat(result.getFirst().questions().getFirst().correct()).isFalse();
        assertThat(result.getFirst().questions().getFirst().selectedAnswer()).isEqualTo("사가");
        assertThat(result.getFirst().questions().getFirst().correctAnswer()).isEqualTo("사과");
        verify(studentRepository).findTrainingHistory(10L, from, to);
    }

    @Test
    void rejectsReversedTrainingHistoryDateRange() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.getTrainingHistory(
                1L,
                10L,
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 5, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void aggregatesVoiceAndGazeReadingSpeedByDate() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        LocalDate firstDate = LocalDate.of(2026, 7, 21);
        LocalDate lastDate = LocalDate.of(2026, 7, 22);
        List<StudentRepository.ReadingSpeedTrainingProjection> rows = List.of(
                readingSpeedRow(firstDate, 10L, 10_000L, 12L, 9_000L),
                readingSpeedRow(firstDate, 20L, 20_000L, 18L, 11_000L),
                readingSpeedRow(lastDate, 12L, 10_000L, 16L, 10_000L)
        );

        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findReadingSpeedTrainings(
                10L,
                firstDate.atStartOfDay(),
                lastDate.plusDays(1).atStartOfDay()
        )).thenReturn(rows);

        var result = studentService.getReadingSpeedTrend(1L, 10L, firstDate, lastDate);

        assertThat(result.unit()).isEqualTo("CORRECT_WORDS_PER_MINUTE");
        assertThat(result.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().voiceSpeed()).isEqualByComparingTo("60.00");
        assertThat(result.points().getFirst().gazeSpeed()).isEqualByComparingTo("90.00");
        assertThat(result.points().getFirst().correctWordCount()).isEqualTo(30L);
        assertThat(result.points().getFirst().gazeWordCount()).isEqualTo(30L);
        assertThat(result.points().getFirst().trainingCount()).isEqualTo(2);
        assertThat(result.points().getLast().voiceSpeed()).isEqualByComparingTo("72.00");
        assertThat(result.points().getLast().gazeSpeed()).isEqualByComparingTo("96.00");
        assertThat(result.voiceChangeRate()).isEqualByComparingTo("20.00");
        assertThat(result.gazeChangeRate()).isEqualByComparingTo("6.67");
    }

    @Test
    void rejectsReadingSpeedRangeWhenFromIsAfterTo() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.getReadingSpeedTrend(
                1L,
                10L,
                LocalDate.of(2026, 7, 23),
                LocalDate.of(2026, 7, 22)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
    }

    private StudentRequest request(String imageUrl) {
        return request(imageUrl, Gender.Boy);
    }

    private StudentRequest request(String imageUrl, Gender gender) {
        return new StudentRequest(
                "학생", LocalDate.of(2016, 3, 10), gender,
                "학교", "보호자", "010-0000-0000", "guardian@test.com", "주소", imageUrl, null
        );
    }

    private StudentEntity student(Long id, String name, LocalDate birthday) {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name(name)
                .birthday(birthday)
                .gender(Gender.Boy)
                .build();
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private StudentRepository.StudentLearningSummaryProjection learningSummary(
            Long studentId,
            LocalDate learningDate
    ) {
        StudentRepository.StudentLearningSummaryProjection row =
                org.mockito.Mockito.mock(StudentRepository.StudentLearningSummaryProjection.class);
        when(row.getStudentId()).thenReturn(studentId);
        when(row.getRecentFinishedAt()).thenReturn(learningDate.atTime(10, 0));
        when(row.getTotalLearningMinutes()).thenReturn(30L);
        when(row.getRecentTrainingName()).thenReturn("문장 읽기");
        return row;
    }

    private MockMultipartFile imageFile(String fileName) {
        return new MockMultipartFile("image", fileName, "image/png", new byte[]{1, 2, 3});
    }

    private StudentRepository.ReadingSpeedTrainingProjection readingSpeedRow(
            LocalDate learningDate,
            Long correctWordCount,
            Long voiceDurationMs,
            Long gazeWordCount,
            Long gazeDurationMs
    ) {
        StudentRepository.ReadingSpeedTrainingProjection row =
                org.mockito.Mockito.mock(StudentRepository.ReadingSpeedTrainingProjection.class);
        when(row.getMeasuredAt()).thenReturn(learningDate.atTime(12, 0));
        when(row.getCorrectWordCount()).thenReturn(correctWordCount);
        when(row.getVoiceDurationMs()).thenReturn(voiceDurationMs);
        when(row.getGazeWordCount()).thenReturn(gazeWordCount);
        when(row.getGazeDurationMs()).thenReturn(gazeDurationMs);
        return row;
    }
}
