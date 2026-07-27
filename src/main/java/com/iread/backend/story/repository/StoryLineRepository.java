package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryLineEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryLineRepository extends JpaRepository<StoryLineEntity, Long> {

    @EntityGraph(attributePaths = "image")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId
             order by line.sequenceNo
            """)
    List<StoryLineEntity> findAllByStoryIdOrderBySequenceNoAsc(@Param("storyId") Long storyId);

    @EntityGraph(attributePaths = "image")
    @Query("""
            select line from StoryLineEntity line
             where line.id = :id and line.scene.story.id = :storyId
            """)
    Optional<StoryLineEntity> findByIdAndStoryId(
            @Param("id") Long id,
            @Param("storyId") Long storyId
    );

    @EntityGraph(attributePaths = "image")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId and line.readAt is null
             order by line.sequenceNo
             limit 1
            """)
    Optional<StoryLineEntity> findFirstByStoryIdAndReadAtIsNullOrderBySequenceNoAsc(
            @Param("storyId") Long storyId
    );

    @EntityGraph(attributePaths = "image")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId
             order by line.sequenceNo desc
             limit 1
            """)
    Optional<StoryLineEntity> findFirstByStoryIdOrderBySequenceNoDesc(
            @Param("storyId") Long storyId
    );
}
