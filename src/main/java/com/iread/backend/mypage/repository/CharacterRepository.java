package com.iread.backend.mypage.repository;

import com.iread.backend.mypage.domain.CharacterEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
    @EntityGraph(attributePaths = {"story", "story.storyTemplate"})
    List<CharacterEntity> findAllByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<CharacterEntity> findFirstByStoryIdOrderByCreatedAtDesc(Long storyId);
}
