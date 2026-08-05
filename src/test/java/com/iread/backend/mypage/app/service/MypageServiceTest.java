package com.iread.backend.mypage.app.service;

import com.iread.backend.mypage.domain.CharacterEntity;
import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryTemplateEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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
    void returnsCharacterContractFields() {
        StudentEntity student = mock(StudentEntity.class);
        CharacterEntity character = mock(CharacterEntity.class);
        StoryEntity story = mock(StoryEntity.class);
        StoryTemplateEntity storyTemplate = mock(StoryTemplateEntity.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 12, 0);
        when(student.getId()).thenReturn(10L);
        when(character.getId()).thenReturn(30L);
        when(character.getStory()).thenReturn(story);
        when(story.getId()).thenReturn(40L);
        when(story.getStoryTemplate()).thenReturn(storyTemplate);
        when(storyTemplate.getTitle()).thenReturn("별빛 숲의 친구");
        when(character.getImageUrl()).thenReturn("/uploads/images/character.png");
        when(character.getName()).thenReturn("책 요정");
        when(character.getCreatedAt()).thenReturn(createdAt);
        when(studentRepository.findByIdAndTeacherId(2L, 1L))
                .thenReturn(Optional.of(student));
        when(characterRepository.findAllByStudentIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(character));

        var result = mypageService.getCharacters(1L, 2L);

        assertThat(result.getFirst().imageUrl()).isEqualTo("/uploads/images/character.png");
        assertThat(result.getFirst().characterId()).isEqualTo(30L);
        assertThat(result.getFirst().storyId()).isEqualTo(40L);
        assertThat(result.getFirst().storyTitle()).isEqualTo("별빛 숲의 친구");
        assertThat(result.getFirst().name()).isEqualTo("책 요정");
        assertThat(result.getFirst().createdAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsUnknownStudent() {
        when(studentRepository.findByIdAndTeacherId(2L, 1L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> mypageService.getCharacters(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
