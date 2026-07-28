package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StorySceneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorySceneRepository extends JpaRepository<StorySceneEntity, Long> {
    long countByStoryId(Long storyId);
}
