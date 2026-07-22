package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<StoryEntity, Long> {

    @EntityGraph(attributePaths = "storyTemplate")
    List<StoryEntity> findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(Long studentId, StoryStatus status);

    @EntityGraph(attributePaths = {"student", "storyTemplate"})
    Optional<StoryEntity> findByIdAndStudentId(Long id, Long studentId);
}
