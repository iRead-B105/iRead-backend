package com.iread.backend.story.admin.repository;

import com.iread.backend.story.admin.domain.StoryPageEditAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryPageEditAuditRepository extends JpaRepository<StoryPageEditAuditEntity, Long> {
}
