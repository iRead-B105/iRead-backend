package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryChoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryChoiceRepository extends JpaRepository<StoryChoiceEntity, Long> {
    Optional<StoryChoiceEntity> findByStoryLineId(Long storyLineId);
}
