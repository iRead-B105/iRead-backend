package com.iread.backend.student.service;

import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.student.domain.Gender;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.dto.req.StudentRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock StudentRepository studentRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock FileStorage fileStorage;

    private StudentServiceImpl studentService;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(
                studentRepository,
                teacherRepository,
                fileStorage,
                JsonMapper.builder().build()
        );
        teacher = new TeacherEntity(
                "teacher@test.com", "encoded-password", "교사", "기관",
                com.iread.backend.teacher.domain.Gender.Female, null
        );
        ReflectionTestUtils.setField(teacher, "id", 1L);
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
    void mapsTrainingResultQuestionsToHistoryResponse() {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher).name("학생").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        StudentRepository.TrainingHistoryProjection row =
                org.mockito.Mockito.mock(StudentRepository.TrainingHistoryProjection.class);
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findTrainingHistory(10L)).thenReturn(List.of(row));
        when(row.getTrainingId()).thenReturn(100L);
        when(row.getLearningDate()).thenReturn(LocalDate.of(2026, 7, 23));
        when(row.getLearningType()).thenReturn("소리 듣고 말하기");
        when(row.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 23, 15, 30));
        when(row.getFinishedAt()).thenReturn(LocalDateTime.of(2026, 7, 23, 15, 30));
        when(row.getAchievement()).thenReturn(new BigDecimal("80.00"));
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

        var result = studentService.getTrainingHistory(1L, 10L);

        assertThat(result.getFirst().trainingId()).isEqualTo(100L);
        assertThat(result.getFirst().questions()).hasSize(1);
        assertThat(result.getFirst().questions().getFirst().question()).isEqualTo("사과를 읽어 보세요.");
        assertThat(result.getFirst().questions().getFirst().correct()).isFalse();
        assertThat(result.getFirst().questions().getFirst().selectedAnswer()).isEqualTo("사가");
        assertThat(result.getFirst().questions().getFirst().correctAnswer()).isEqualTo("사과");
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

        assertThat(result.unit()).isEqualTo("WORDS_PER_MINUTE");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().voiceSpeed()).isEqualByComparingTo("60.00");
        assertThat(result.points().getFirst().gazeSpeed()).isEqualByComparingTo("90.00");
        assertThat(result.points().getFirst().voiceWordCount()).isEqualTo(30L);
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
        return new StudentRequest(
                "학생", LocalDate.of(2016, 3, 10), Gender.Boy,
                "학교", "보호자", "010-0000-0000", "guardian@test.com", "주소", imageUrl
        );
    }

    private MockMultipartFile imageFile(String fileName) {
        return new MockMultipartFile("image", fileName, "image/png", new byte[]{1, 2, 3});
    }

    private StudentRepository.ReadingSpeedTrainingProjection readingSpeedRow(
            LocalDate learningDate,
            Long voiceWordCount,
            Long voiceDurationMs,
            Long gazeWordCount,
            Long gazeDurationMs
    ) {
        StudentRepository.ReadingSpeedTrainingProjection row =
                org.mockito.Mockito.mock(StudentRepository.ReadingSpeedTrainingProjection.class);
        when(row.getLearningDate()).thenReturn(learningDate);
        when(row.getVoiceWordCount()).thenReturn(voiceWordCount);
        when(row.getVoiceDurationMs()).thenReturn(voiceDurationMs);
        when(row.getGazeWordCount()).thenReturn(gazeWordCount);
        when(row.getGazeDurationMs()).thenReturn(gazeDurationMs);
        return row;
    }
}
