package com.iread.backend.wordattempt.repository;

import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WordAttemptLogRepository extends JpaRepository<WordAttemptLogEntity, Long> {
    @Query(value = """
            SELECT w.id AS wordId,
                   w.content AS wordName,
                   COUNT(*) AS attemptCount,
                   SUM(CASE WHEN wal.is_correct = false THEN 1 ELSE 0 END) AS incorrectCount
              FROM word_attempt_logs wal
              JOIN words w ON w.id = wal.word_id
             WHERE wal.student_id = :studentId
               AND wal.created_at >= :fromDateTime
               AND wal.created_at < :toDateTime
             GROUP BY w.id, w.content
            """, nativeQuery = true)
    List<IncorrectWordProjection> findIncorrectWordStats(
            @Param("studentId") Long studentId,
            @Param("fromDateTime") LocalDateTime from,
            @Param("toDateTime") LocalDateTime to
    );

    interface IncorrectWordProjection {
        Long getWordId();
        String getWordName();
        Long getAttemptCount();
        Long getIncorrectCount();
    }
}
