package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppStudentProfileServiceTest {
    @Mock StudentRepository studentRepository;
    @InjectMocks AppStudentProfileService service;

    @Test
    void returnsOwnedStudentProfile() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(20L);
        when(student.getName()).thenReturn("샛별");
        when(student.getBirthday()).thenReturn(LocalDate.now().minusYears(8));
        when(student.getImageUrl()).thenReturn("/profiles/20.png");

        var response = service.getProfile(1L, 20L);

        assertThat(response.studentId()).isEqualTo("20");
        assertThat(response.name()).isEqualTo("샛별");
        assertThat(response.age()).isEqualTo(8);
        assertThat(response.profileImageUrl()).isEqualTo("/profiles/20.png");
    }

    @Test
    void rejectsStudentOutsideTeacherOwnership() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
