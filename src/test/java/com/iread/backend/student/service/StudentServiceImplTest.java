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

import java.time.LocalDate;
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
        studentService = new StudentServiceImpl(studentRepository, teacherRepository, fileStorage);
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

    private StudentRequest request(String imageUrl) {
        return new StudentRequest(
                "학생", LocalDate.of(2016, 3, 10), Gender.Boy,
                "학교", "보호자", "010-0000-0000", "guardian@test.com", "주소", imageUrl
        );
    }

    private MockMultipartFile imageFile(String fileName) {
        return new MockMultipartFile("image", fileName, "image/png", new byte[]{1, 2, 3});
    }
}
