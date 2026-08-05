package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryChoiceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryChoiceRepository extends JpaRepository<StoryChoiceEntity, Long> {
    Optional<StoryChoiceEntity> findByStoryLineId(Long storyLineId);

    @EntityGraph(attributePaths = "storyLine")
    List<StoryChoiceEntity> findAllByStoryLineIdIn(List<Long> storyLineIds);
}
