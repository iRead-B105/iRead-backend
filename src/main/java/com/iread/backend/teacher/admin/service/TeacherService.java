package com.iread.backend.teacher.admin.service;

import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.admin.dto.res.TeacherInfoResponse;
import com.iread.backend.teacher.admin.dto.req.UpdateTeacherProfileRequest;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final FileStorage fileStorage;

    public TeacherInfoResponse getTeacherInfo(Long teacherId) {
        TeacherEntity teacher = findTeacher(teacherId);

        return TeacherInfoResponse.from(teacher, teacher.getImageUrl());
    }

    @Transactional
    public TeacherInfoResponse updateProfile(Long teacherId, UpdateTeacherProfileRequest request) {
        TeacherEntity teacher = findTeacher(teacherId);
        teacher.updateProfile(request.name(), request.organization(), request.gender());
        return TeacherInfoResponse.from(teacher, teacher.getImageUrl());
    }

    @Transactional
    public TeacherInfoResponse updateProfileImage(Long teacherId, MultipartFile imageFile) {
        TeacherEntity teacher = findTeacher(teacherId);
        String oldImageUrl = teacher.getImageUrl();
        StoredFile storedFile = fileStorage.store(imageFile);
        teacher.updateImageUrl(storedFile.url());
        try {
            teacherRepository.saveAndFlush(teacher);
        } catch (RuntimeException exception) {
            fileStorage.delete(storedFile.storeFileName());
            throw exception;
        }
        if (oldImageUrl != null) {
            fileStorage.delete(fileNameOf(oldImageUrl));
        }
        return TeacherInfoResponse.from(teacher, teacher.getImageUrl());
    }

    private TeacherEntity findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("교사를 찾을 수 없습니다."));
    }

    private String fileNameOf(String imageUrl) {
        int slash = imageUrl.lastIndexOf('/');
        return slash < 0 ? imageUrl : imageUrl.substring(slash + 1);
    }
}
