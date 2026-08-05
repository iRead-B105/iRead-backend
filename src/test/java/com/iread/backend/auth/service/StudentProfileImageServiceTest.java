package com.iread.backend.auth.service;

import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.AuthRole;
import com.iread.backend.auth.security.JwtTokenService;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.LoadedFile;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileImageServiceTest {

    private static final String FILE_NAME = "8efbe8c2-5c48-4360-b7c0-c2956bbceda9.png";

    @Mock StudentRepository studentRepository;
    @Mock FileStorage fileStorage;

    private StudentProfileImageService service;
    private StudentEntity student;

    @BeforeEach
    void setUp() {
        service = new StudentProfileImageService(studentRepository, fileStorage);
        TeacherEntity teacher = new TeacherEntity(
                "teacher@example.com",
                "password",
                "교사",
                "기관",
                null,
                null
        );
        ReflectionTestUtils.setField(teacher, "id", 10L);
        student = StudentEntity.builder()
                .teacher(teacher)
                .name("엘리스")
                .imageUrl("/uploads/images/" + FILE_NAME)
                .build();
        ReflectionTestUtils.setField(student, "id", 20L);
    }

    @Test
    void bootstrapTokenLoadsOnlyLinkedStudentProfileImage() {
        AuthPrincipal principal = principal(AuthRole.TEACHER, null, JwtTokenService.BOOTSTRAP_AUDIENCE);
        LoadedFile expected = new LoadedFile(new byte[]{1, 2, 3}, "image/png");
        when(studentRepository.findByIdAndTeacherId(20L, 10L)).thenReturn(Optional.of(student));
        when(fileStorage.load(FILE_NAME)).thenReturn(expected);

        assertThat(service.load(principal, "20")).isSameAs(expected);
        verify(fileStorage).load(FILE_NAME);
    }

    @Test
    void learningTokenCannotLoadAnotherStudentProfileImage() {
        AuthPrincipal principal = principal(AuthRole.STUDENT, 20L, JwtTokenService.LEARNING_AUDIENCE);

        assertThatThrownBy(() -> service.load(principal, "21"))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("STUDENT_NOT_FOUND");
        verify(studentRepository, never()).findByIdAndTeacherId(21L, 10L);
    }

    private AuthPrincipal principal(AuthRole role, Long studentId, String audience) {
        return new AuthPrincipal(
                10L,
                studentId,
                role,
                audience,
                "token-id",
                Instant.now().plusSeconds(60)
        );
    }
}
