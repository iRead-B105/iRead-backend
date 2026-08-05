package com.iread.backend.auth.service;

import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.AuthRole;
import com.iread.backend.auth.security.JwtTokenService;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.LoadedFile;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StudentProfileImageService {

    private static final Pattern LOCAL_PROFILE_IMAGE = Pattern.compile(
            "^/uploads/images/([0-9a-f-]{36}\\.(?:png|jpg|jpeg))$"
    );

    private final StudentRepository studentRepository;
    private final FileStorage fileStorage;

    public StudentProfileImageService(
            StudentRepository studentRepository,
            FileStorage fileStorage
    ) {
        this.studentRepository = studentRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public LoadedFile load(AuthPrincipal principal, String rawStudentId) {
        validatePrincipal(principal);
        Long studentId = parseStudentId(rawStudentId);
        if (principal.role() == AuthRole.STUDENT && !studentId.equals(principal.studentId())) {
            throw studentNotFound();
        }

        StudentEntity student = studentRepository.findByIdAndTeacherId(studentId, principal.id())
                .orElseThrow(this::studentNotFound);
        Matcher matcher = LOCAL_PROFILE_IMAGE.matcher(
                student.getImageUrl() == null ? "" : student.getImageUrl()
        );
        if (!matcher.matches()) {
            throw new ResourceNotFoundException("아동 프로필 이미지를 찾을 수 없습니다.");
        }
        return fileStorage.load(matcher.group(1));
    }

    private void validatePrincipal(AuthPrincipal principal) {
        boolean bootstrapTeacher = principal != null
                && principal.role() == AuthRole.TEACHER
                && JwtTokenService.BOOTSTRAP_AUDIENCE.equals(principal.audience());
        boolean learningStudent = principal != null
                && principal.role() == AuthRole.STUDENT
                && JwtTokenService.LEARNING_AUDIENCE.equals(principal.audience());
        if (!bootstrapTeacher && !learningStudent) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    "INVALID_TOKEN_AUDIENCE",
                    "아동 프로필 이미지 조회 권한이 없습니다."
            );
        }
    }

    private Long parseStudentId(String rawStudentId) {
        try {
            return Long.valueOf(rawStudentId);
        } catch (NumberFormatException exception) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STUDENT_ID",
                    "아동 ID 형식이 올바르지 않습니다."
            );
        }
    }

    private AuthException studentNotFound() {
        return new AuthException(
                HttpStatus.NOT_FOUND,
                "STUDENT_NOT_FOUND",
                "연결된 아동을 찾을 수 없습니다."
        );
    }
}
