package com.iread.backend.mypage.app.service;

import com.iread.backend.mypage.app.dto.res.CharacterResponse;
import com.iread.backend.mypage.repository.CharacterRepository;
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

    public List<CharacterResponse> getCharacters(Long teacherId, String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            throw new IllegalArgumentException("학생 코드는 필수입니다.");
        }
        StudentEntity student = studentRepository.findByStudentCodeAndTeacherId(studentCode, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

        return characterRepository.findAllByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(character -> new CharacterResponse(
                        character.getImageUrl(),
                        fileNameOf(character.getImageUrl())
                ))
                .toList();
    }

    private String fileNameOf(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        int slash = imageUrl.lastIndexOf('/');
        return slash < 0 ? imageUrl : imageUrl.substring(slash + 1);
    }
}
