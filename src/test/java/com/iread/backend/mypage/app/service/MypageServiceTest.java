package com.iread.backend.mypage.app.service;

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
    void returnsCharacterImageUrl() {
        StudentEntity student = mock(StudentEntity.class);
        CharacterEntity character = mock(CharacterEntity.class);
        when(student.getId()).thenReturn(10L);
        when(character.getImageUrl()).thenReturn("/uploads/images/character.png");
        when(studentRepository.findByIdAndTeacherId(2L, 1L))
                .thenReturn(Optional.of(student));
        when(characterRepository.findAllByStudentIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(character));

        var result = mypageService.getCharacters(1L, 2L);

        assertThat(result.getFirst().imageUrl()).isEqualTo("/uploads/images/character.png");
        assertThat(result.getFirst().imageName()).isEqualTo("character.png");
    }

    @Test
    void rejectsUnknownStudent() {
        when(studentRepository.findByIdAndTeacherId(2L, 1L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> mypageService.getCharacters(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
