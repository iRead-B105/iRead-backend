package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryLineEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryLineRepository extends JpaRepository<StoryLineEntity, Long> {

    @EntityGraph(attributePaths = "image")
    List<StoryLineEntity> findAllByStoryIdOrderBySequenceNoAsc(Long storyId);

    @EntityGraph(attributePaths = "image")
    Optional<StoryLineEntity> findByIdAndStoryId(Long id, Long storyId);

    @EntityGraph(attributePaths = "image")
    Optional<StoryLineEntity> findFirstByStoryIdAndReadAtIsNullOrderBySequenceNoAsc(Long storyId);

    @EntityGraph(attributePaths = "image")
    Optional<StoryLineEntity> findFirstByStoryIdOrderBySequenceNoDesc(Long storyId);
}
