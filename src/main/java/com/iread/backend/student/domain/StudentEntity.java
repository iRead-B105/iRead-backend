package com.iread.backend.student.domain;

import com.iread.backend.global.domain.ImageEntity;
import com.iread.backend.teacher.domain.TeacherEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_students_student_code",
                        columnNames = "student_code"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 담당 지도자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherEntity teacher;

    @Column(length = 10)
    private String name;

    @Column(name = "student_code", nullable = false, length = 10)
    private String studentCode;

    @Column
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 20)
    private String school;

    @Column(length = 10)
    private String guardian;

    @Column(name = "guardian_contact", length = 20)
    private String guardianContact;

    @Column(name = "guardian_email", length = 50)
    private String guardianEmail;

    @Column(length = 100)
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 프로필 사진
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private ImageEntity image;

    @Builder
    public StudentEntity(
            TeacherEntity teacher,
            String name,
            String studentCode,
            LocalDate birthday,
            Gender gender,
            String school,
            String guardian,
            String guardianContact,
            String guardianEmail,
            String address,
            ImageEntity image
    ) {
        this.teacher = teacher;
        this.name = name;
        this.studentCode = studentCode;
        this.birthday = birthday;
        this.gender = gender;
        this.school = school;
        this.guardian = guardian;
        this.guardianContact = guardianContact;
        this.guardianEmail = guardianEmail;
        this.address = address;
        this.image = image;
    }

    public void update(
            String name,
            String studentCode,
            LocalDate birthday,
            Gender gender,
            String school,
            String guardian,
            String guardianContact,
            String guardianEmail,
            String address,
            ImageEntity image,
            boolean updateImage
    ) {
        if (name != null) this.name = name;
        if (studentCode != null) this.studentCode = studentCode;
        if (birthday != null) this.birthday = birthday;
        if (gender != null) this.gender = gender;
        if (school != null) this.school = school;
        if (guardian != null) this.guardian = guardian;
        if (guardianContact != null) this.guardianContact = guardianContact;
        if (guardianEmail != null) this.guardianEmail = guardianEmail;
        if (address != null) this.address = address;
        if (updateImage) this.image = image;
    }
}
