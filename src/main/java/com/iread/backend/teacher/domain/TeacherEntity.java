package com.iread.backend.teacher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "name", length = 10)
    private String name;

    @Column(name = "organization", length = 100)
    private String organization;

    @Column(name = "gender", length = 10)
    private String gender;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "images_id")
    private Long imagesId;

    public TeacherEntity(String email, String password, String name, String organization, String gender, Long imagesId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.organization = organization;
        this.gender = gender;
        this.imagesId = imagesId;
    }
}
