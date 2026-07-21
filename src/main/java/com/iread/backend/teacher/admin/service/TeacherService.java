package com.iread.backend.teacher.admin.service;

import com.iread.backend.global.repository.ImageRepository;
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
    private final ImageRepository imageRepository;

    public TeacherInfoResponse getTeacherInfo(Long teacherId) {
        TeacherEntity teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("교사를 찾을 수 없습니다."));

        String profileImageUrl = teacher.getImagesId() == null
                ? null
                : imageRepository.findById(teacher.getImagesId())
                        .map(image -> image.getUrl())
                        .orElse(null);

        return TeacherInfoResponse.from(teacher, profileImageUrl);
    }
}
