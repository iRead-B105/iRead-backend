package com.iread.backend.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class StudentResourceAccessPolicy {

    public void requireSameStudent(Long authenticatedStudentId, Long requestedStudentId) {
        if (authenticatedStudentId == null || !Objects.equals(authenticatedStudentId, requestedStudentId)) {
            throw new AccessDeniedException("다른 아동의 리소스에 접근할 수 없습니다.");
        }
    }
}
