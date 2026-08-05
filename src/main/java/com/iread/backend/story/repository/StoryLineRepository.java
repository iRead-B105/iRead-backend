package com.iread.backend.story.repository;

import com.iread.backend.story.domain.StoryLineEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryLineRepository extends JpaRepository<StoryLineEntity, Long> {

    @EntityGraph(attributePaths = "scene")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId
             order by line.scene.sequenceNo, line.sequenceNo
            """)
    List<StoryLineEntity> findAllByStoryIdOrderBySequenceNoAsc(@Param("storyId") Long storyId);

    @EntityGraph(attributePaths = {"scene", "scene.story"})
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id in :storyIds
             order by line.scene.story.id, line.scene.sequenceNo, line.sequenceNo, line.id
            """)
    List<StoryLineEntity> findAllByStoryIdInOrderBySequenceNoAsc(
            @Param("storyIds") List<Long> storyIds
    );

    @EntityGraph(attributePaths = "scene")
    @Query("""
            select line from StoryLineEntity line
             where line.id = :id and line.scene.story.id = :storyId
            """)
    Optional<StoryLineEntity> findByIdAndStoryId(
            @Param("id") Long id,
            @Param("storyId") Long storyId
    );

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "scene")
    @Query("""
            select line from StoryLineEntity line
             where line.id = :id and line.scene.story.id = :storyId
            """)
    Optional<StoryLineEntity> findByIdAndStoryIdForUpdate(
            @Param("id") Long id,
            @Param("storyId") Long storyId
    );

    @EntityGraph(attributePaths = "scene")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId and line.readAt is null
             order by line.scene.sequenceNo, line.sequenceNo
             limit 1
            """)
    Optional<StoryLineEntity> findFirstByStoryIdAndReadAtIsNullOrderBySequenceNoAsc(
            @Param("storyId") Long storyId
    );

    @EntityGraph(attributePaths = "scene")
    @Query("""
            select line from StoryLineEntity line
             where line.scene.story.id = :storyId
             order by line.scene.sequenceNo desc, line.sequenceNo desc
             limit 1
            """)
    Optional<StoryLineEntity> findFirstByStoryIdOrderBySequenceNoDesc(
            @Param("storyId") Long storyId
    );
}
