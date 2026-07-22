package com.iread.backend.mypage.app.service;

import com.iread.backend.global.domain.ImageEntity;
import com.iread.backend.mypage.domain.CharacterEntity;
import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MypageServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock CharacterRepository characterRepository;
    @InjectMocks MypageService mypageService;

    @Test
    void 획득한_캐릭터의_이미지_URL과_원본파일명을_반환한다() {
        StudentEntity student = mock(StudentEntity.class);
        CharacterEntity character = mock(CharacterEntity.class);
        ImageEntity image = mock(ImageEntity.class);
        when(student.getId()).thenReturn(10L);
        when(character.getImage()).thenReturn(image);
        when(image.getUrl()).thenReturn("/uploads/images/character.png");
        when(image.getOriginalFileName()).thenReturn("고양이.png");
        when(studentRepository.findByStudentCodeAndTeacherId("ST00000001", 1L))
                .thenReturn(Optional.of(student));
        when(characterRepository.findAllByStudentIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(character));

        var result = mypageService.getCharacters(1L, "ST00000001");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().imageUrl()).isEqualTo("/uploads/images/character.png");
        assertThat(result.getFirst().imageName()).isEqualTo("고양이.png");
    }

    @Test
    void 담당_학생이_아니면_캐릭터를_조회할_수_없다() {
        when(studentRepository.findByStudentCodeAndTeacherId("ST00000001", 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mypageService.getCharacters(1L, "ST00000001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학생을 찾을 수 없습니다.");
    }
}
