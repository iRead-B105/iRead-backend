package com.iread.backend.training.repository;

import com.iread.backend.training.domain.WordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordRepository extends JpaRepository<WordEntity, Long> {
    Optional<WordEntity> findByContent(String content);
}
