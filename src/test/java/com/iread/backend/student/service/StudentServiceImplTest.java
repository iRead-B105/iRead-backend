package com.iread.backend.student.service;

import com.iread.backend.global.domain.ImageEntity;
import com.iread.backend.global.repository.ImageRepository;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private FileStorage fileStorage;

    private StudentServiceImpl studentService;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(studentRepository, teacherRepository, imageRepository, fileStorage);
        teacher = new TeacherEntity(
                "teacher@test.com",
                "encoded-password",
                "교사",
                "기관",
                com.iread.backend.teacher.domain.Gender.Female,
                null
        );
        ReflectionTestUtils.setField(teacher, "id", 1L);
    }

    @Test
    void 학생_목록에_만나이와_학습요약을_반환한다() {
        StudentEntity student = student(10L, LocalDate.now().minusYears(10).minusDays(1));
        StudentRepository.StudentLearningSummaryProjection summary = learningSummary(
                10L,
                LocalDateTime.of(2026, 7, 20, 15, 30),
                125L,
                "받침 읽기"
        );
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.findLearningSummaries(1L)).thenReturn(List.of(summary));
        when(studentRepository.findAllByTeacherIdOrderByIdAsc(1L)).thenReturn(List.of(student));

        var result = studentService.getStudents(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(10L);
        assertThat(result.getFirst().age()).isEqualTo(10);
        assertThat(result.getFirst().recentLearningDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(result.getFirst().totalLearningTime()).isEqualTo(125L);
        assertThat(result.getFirst().recentTraining()).isEqualTo("받침 읽기");
    }

    @Test
    void 학생_상세는_교사에게_소속된_학생만_반환한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        var result = studentService.getStudent(1L, 10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.studentCode()).isEqualTo("ST00000001");
        assertThat(result.gender()).isEqualTo(Gender.Boy);
        assertThat(result.imageId()).isNull();
    }

    @Test
    void 다른_교사의_학생은_조회할_수_없다() {
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudent(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학생을 찾을 수 없습니다.");
    }

    @Test
    void 학생을_등록하고_없는_이미지는_null로_처리한다() {
        StudentRequest request = request("학생", "ST00000002", 999L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByStudentCode("ST00000002")).thenReturn(false);
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        studentService.createStudent(1L, request);

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacher()).isSameAs(teacher);
        assertThat(captor.getValue().getStudentCode()).isEqualTo("ST00000002");
        assertThat(captor.getValue().getImage()).isNull();
    }

    @Test
    void 학생_등록시_업로드한_이미지를_저장하고_연결한다() {
        StudentRequest request = request("학생", "ST00000002", null);
        MockMultipartFile imageFile = imageFile("profile.png");
        StoredFile storedFile = new StoredFile(
                "profile.png", "stored-profile.png", imageFile.getSize(), "/uploads/images/stored-profile.png"
        );
        ImageEntity image = image(20L, storedFile);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByStudentCode("ST00000002")).thenReturn(false);
        when(fileStorage.store(imageFile)).thenReturn(storedFile);
        when(imageRepository.save(org.mockito.ArgumentMatchers.any(ImageEntity.class))).thenReturn(image);

        studentService.createStudent(1L, request, imageFile);

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getImage()).isSameAs(image);
        verify(fileStorage).store(imageFile);
    }

    @Test
    void 학생_등록실패시_새로_업로드한_파일을_삭제한다() {
        StudentRequest request = request("학생", "ST00000002", null);
        MockMultipartFile imageFile = imageFile("profile.png");
        StoredFile storedFile = new StoredFile(
                "profile.png", "stored-profile.png", imageFile.getSize(), "/uploads/images/stored-profile.png"
        );
        ImageEntity image = image(20L, storedFile);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByStudentCode("ST00000002")).thenReturn(false);
        when(fileStorage.store(imageFile)).thenReturn(storedFile);
        when(imageRepository.save(org.mockito.ArgumentMatchers.any(ImageEntity.class))).thenReturn(image);
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(studentRepository).save(org.mockito.ArgumentMatchers.any(StudentEntity.class));

        assertThatThrownBy(() -> studentService.createStudent(1L, request, imageFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 저장 실패");
        verify(fileStorage).delete("stored-profile.png");
    }

    @Test
    void 중복_학생코드는_등록할_수_없다() {
        StudentRequest request = request("학생", "ST00000002", null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByStudentCode("ST00000002")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 학생 코드입니다.");
        verify(studentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void null이_아닌_필드만_수정한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        StudentRequest request = new StudentRequest(
                "수정 이름", null, null, null, null,
                null, null, null, null, null
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        studentService.updateStudent(1L, 10L, request);

        assertThat(student.getName()).isEqualTo("수정 이름");
        assertThat(student.getStudentCode()).isEqualTo("ST00000001");
        assertThat(student.getBirthday()).isEqualTo(LocalDate.of(2016, 3, 10));
    }

    @Test
    void 학생_수정시_새_이미지로_교체하고_기존_이미지를_삭제한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        StoredFile oldStoredFile = new StoredFile(
                "old.png", "old-stored.png", 10L, "/uploads/images/old-stored.png"
        );
        ImageEntity oldImage = image(20L, oldStoredFile);
        ReflectionTestUtils.setField(student, "image", oldImage);

        MockMultipartFile newImageFile = imageFile("new.png");
        StoredFile newStoredFile = new StoredFile(
                "new.png", "new-stored.png", newImageFile.getSize(), "/uploads/images/new-stored.png"
        );
        ImageEntity newImage = image(21L, newStoredFile);
        StudentRequest request = new StudentRequest(
                null, null, null, null, null,
                null, null, null, null, null
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(fileStorage.store(newImageFile)).thenReturn(newStoredFile);
        when(imageRepository.save(org.mockito.ArgumentMatchers.any(ImageEntity.class))).thenReturn(newImage);

        studentService.updateStudent(1L, 10L, request, newImageFile);

        assertThat(student.getImage()).isSameAs(newImage);
        verify(imageRepository).delete(oldImage);
        verify(fileStorage).delete("old-stored.png");
    }

    @Test
    void 학생_삭제시_훈련과_일일커리큘럼을_먼저_삭제한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));

        studentService.deleteStudent(1L, 10L);

        InOrder order = inOrder(studentRepository);
        order.verify(studentRepository).deleteTrainingsByStudentId(10L);
        order.verify(studentRepository).deleteDailyCurriculumsByStudentId(10L);
        order.verify(studentRepository).delete(student);
    }

    @Test
    void 정확도_추이_projection을_응답으로_변환한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        StudentRepository.AccuracyTrendProjection projection = accuracy(
                LocalDate.of(2026, 7, 20), new BigDecimal("87.50")
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findAccuracyTrend(10L)).thenReturn(List.of(projection));

        var result = studentService.getAccuracyTrend(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().date()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(result.getFirst().accuracy()).isEqualByComparingTo("87.50");
    }

    @Test
    void 학습기록에_훈련명과_시작종료시각과_정답률을_반환한다() {
        StudentEntity student = student(10L, LocalDate.of(2016, 3, 10));
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 20, 14, 0);
        LocalDateTime finishedAt = startedAt.plusMinutes(20);
        StudentRepository.TrainingHistoryProjection projection = history(
                LocalDate.of(2026, 7, 20), "받침 읽기", startedAt, finishedAt, new BigDecimal("92.00")
        );
        when(studentRepository.findByIdAndTeacherId(10L, 1L)).thenReturn(Optional.of(student));
        when(studentRepository.findTrainingHistory(10L)).thenReturn(List.of(projection));

        var result = studentService.getTrainingHistory(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().learningType()).isEqualTo("받침 읽기");
        assertThat(result.getFirst().startedAt()).isEqualTo(startedAt);
        assertThat(result.getFirst().finishedAt()).isEqualTo(finishedAt);
        assertThat(result.getFirst().achievement()).isEqualByComparingTo("92.00");
    }

    private StudentEntity student(Long id, LocalDate birthday) {
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name("학생")
                .studentCode("ST00000001")
                .birthday(birthday)
                .gender(Gender.Boy)
                .school("학교")
                .build();
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private StudentRequest request(String name, String studentCode, Long imageId) {
        return new StudentRequest(
                name, studentCode, LocalDate.of(2016, 3, 10), Gender.Boy,
                "학교", "보호자", "010-0000-0000", "guardian@test.com", "주소", imageId
        );
    }

    private MockMultipartFile imageFile(String fileName) {
        return new MockMultipartFile("image", fileName, "image/png", new byte[]{1, 2, 3});
    }

    private ImageEntity image(Long id, StoredFile storedFile) {
        ImageEntity image = ImageEntity.builder()
                .originalFileName(storedFile.originalFileName())
                .storeFileName(storedFile.storeFileName())
                .fileSize(storedFile.fileSize())
                .url(storedFile.url())
                .build();
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }

    private StudentRepository.StudentLearningSummaryProjection learningSummary(
            Long studentId,
            LocalDateTime recentFinishedAt,
            Long totalLearningMinutes,
            String recentTrainingName
    ) {
        return new StudentRepository.StudentLearningSummaryProjection() {
            public Long getStudentId() { return studentId; }
            public LocalDateTime getRecentFinishedAt() { return recentFinishedAt; }
            public Long getTotalLearningMinutes() { return totalLearningMinutes; }
            public String getRecentTrainingName() { return recentTrainingName; }
        };
    }

    private StudentRepository.AccuracyTrendProjection accuracy(LocalDate date, BigDecimal value) {
        return new StudentRepository.AccuracyTrendProjection() {
            public LocalDate getLearningDate() { return date; }
            public BigDecimal getAccuracy() { return value; }
        };
    }

    private StudentRepository.TrainingHistoryProjection history(
            LocalDate date,
            String type,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            BigDecimal achievement
    ) {
        return new StudentRepository.TrainingHistoryProjection() {
            public LocalDate getLearningDate() { return date; }
            public String getLearningType() { return type; }
            public LocalDateTime getStartedAt() { return startedAt; }
            public LocalDateTime getFinishedAt() { return finishedAt; }
            public BigDecimal getAchievement() { return achievement; }
        };
    }
}
