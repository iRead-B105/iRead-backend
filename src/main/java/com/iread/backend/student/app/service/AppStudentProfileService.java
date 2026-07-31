package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.app.dto.res.AppStudentProfileResponse;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppStudentProfileService {
    private final StudentRepository studentRepository;

    public AppStudentProfileResponse getProfile(Long teacherId, Long studentId) {
        StudentEntity student = studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        return AppStudentProfileResponse.from(student, LocalDate.now());
    }
}
