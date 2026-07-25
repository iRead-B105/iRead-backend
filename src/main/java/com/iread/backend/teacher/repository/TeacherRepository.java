package com.iread.backend.teacher.repository;

import com.iread.backend.teacher.domain.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {

    Optional<TeacherEntity> findByEmail(String email);

    Optional<TeacherEntity> findByLoginId(String loginId);

    Optional<TeacherEntity> findByLoginIdAndEmail(String loginId, String email);

    Optional<TeacherEntity> findByNameAndEmail(String name, String email);

    boolean existsByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailAndIdNot(String email, Long id);
}
