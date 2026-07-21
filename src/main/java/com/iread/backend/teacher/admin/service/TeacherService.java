package com.iread.backend.teacher.admin.service;

import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.admin.dto.res.TeacherInfoResponse;
import com.iread.backend.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherInfoResponse getTeacherInfo(Long teacherId) {
        TeacherEntity teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("교사를 찾을 수 없습니다."));

        String profileImageUrl = teacher.getImage() == null ? null : teacher.getImage().getUrl();

        return TeacherInfoResponse.from(teacher, profileImageUrl);
    }
}
