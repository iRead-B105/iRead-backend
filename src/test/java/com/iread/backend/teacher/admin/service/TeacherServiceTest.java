package com.iread.backend.teacher.admin.service;

import com.iread.backend.teacher.domain.Gender;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {
    @Mock TeacherRepository teacherRepository;
    @InjectMocks TeacherService teacherService;

    @Test
    void returnsProfileImageUrl() {
        TeacherEntity teacher = teacher(1L, "/uploads/images/profile.png");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        var result = teacherService.getTeacherInfo(1L);

        assertThat(result.email()).isEqualTo("teacher@test.com");
        assertThat(result.gender()).isEqualTo(Gender.Female);
        assertThat(result.profileImageUrl()).isEqualTo("/uploads/images/profile.png");
    }

    @Test
    void returnsNullWithoutProfileImage() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, null)));
        assertThat(teacherService.getTeacherInfo(1L).profileImageUrl()).isNull();
    }

    @Test
    void rejectsUnknownTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> teacherService.getTeacherInfo(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TeacherEntity teacher(Long id, String imageUrl) {
        TeacherEntity teacher = new TeacherEntity(
                "teacher@test.com", "password", "교사", "한국대학교", Gender.Female, imageUrl
        );
        ReflectionTestUtils.setField(teacher, "id", id);
        return teacher;
    }
}
