package com.iread.backend.mypage.app.service;

import com.iread.backend.mypage.app.dto.res.CharacterResponse;
import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {
    private final StudentRepository studentRepository;
    private final CharacterRepository characterRepository;

    public List<CharacterResponse> getCharacters(Long teacherId, Long studentId) {
        StudentEntity student = studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        return characterRepository.findAllByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(character -> new CharacterResponse(
                        character.getId(),
                        character.getStory().getId(),
                        character.getStory().getStoryTemplate().getTitle(),
                        character.getImageUrl(),
                        character.getName(),
                        character.getCreatedAt()
                ))
                .toList();
    }
}
