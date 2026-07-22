package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryTemplateRepository extends JpaRepository<StoryTemplateEntity, Long> {
    List<StoryTemplateEntity> findAllByOrderByIdAsc();
}
