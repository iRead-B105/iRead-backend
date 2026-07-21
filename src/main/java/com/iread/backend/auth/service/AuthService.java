package com.iread.backend.auth.service;

import com.iread.backend.auth.dto.LoginRequest;
import com.iread.backend.auth.dto.SignUpRequest;
import com.iread.backend.auth.dto.TeacherAuthResponse;
import com.iread.backend.auth.session.LoginTeacher;
import com.iread.backend.auth.session.SessionConst;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherAuthResponse signUp(SignUpRequest request) {
        if (teacherRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        TeacherEntity teacher = new TeacherEntity(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.organization(),
                request.gender(),
                request.imagesId()
        );

        return TeacherAuthResponse.from(teacherRepository.save(teacher));
    }

    @Transactional(readOnly = true)
    public TeacherAuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        TeacherEntity teacher = teacherRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), teacher.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        LoginTeacher loginTeacher = LoginTeacher.from(teacher);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionConst.LOGIN_TEACHER, loginTeacher);

        return TeacherAuthResponse.from(loginTeacher);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public TeacherAuthResponse getLoginTeacher(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object attribute = session.getAttribute(SessionConst.LOGIN_TEACHER);
        if (!(attribute instanceof LoginTeacher loginTeacher)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        return TeacherAuthResponse.from(loginTeacher);
    }
}
