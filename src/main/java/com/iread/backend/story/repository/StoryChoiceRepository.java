package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryChoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryChoiceRepository extends JpaRepository<StoryChoiceEntity, Long> {
    boolean existsByStoryLineId(Long storyLineId);
    List<StoryChoiceEntity> findAllByStoryLineStoryId(Long storyId);
}
