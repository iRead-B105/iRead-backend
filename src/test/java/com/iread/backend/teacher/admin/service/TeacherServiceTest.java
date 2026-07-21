package com.iread.backend.teacher.admin.service;

import com.iread.backend.global.domain.ImageEntity;
import com.iread.backend.global.repository.ImageRepository;
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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void 교사_정보와_프로필_이미지_URL을_반환한다() {
        TeacherEntity teacher = teacher(1L, 10L);
        ImageEntity image = newImage("/uploads/images/profile.png");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(imageRepository.findById(10L)).thenReturn(Optional.of(image));

        var result = teacherService.getTeacherInfo(1L);

        assertThat(result.name()).isEqualTo("교사");
        assertThat(result.organization()).isEqualTo("한글학교");
        assertThat(result.email()).isEqualTo("teacher@test.com");
        assertThat(result.gender()).isEqualTo("Female");
        assertThat(result.profileImageUrl()).isEqualTo("/uploads/images/profile.png");
    }

    @Test
    void 프로필_이미지가_없으면_URL은_null이다() {
        TeacherEntity teacher = teacher(1L, null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        var result = teacherService.getTeacherInfo(1L);

        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    void 존재하지_않는_교사는_조회할_수_없다() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherInfo(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("교사를 찾을 수 없습니다.");
    }

    private TeacherEntity teacher(Long id, Long imageId) {
        TeacherEntity teacher = new TeacherEntity(
                "teacher@test.com", "password", "교사", "한글학교", "Female", imageId
        );
        ReflectionTestUtils.setField(teacher, "id", id);
        return teacher;
    }

    private ImageEntity newImage(String url) {
        ImageEntity image = mock(ImageEntity.class);
        when(image.getUrl()).thenReturn(url);
        return image;
    }
}
