package com.iread.backend.wordattempt.repository;

import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordAttemptLogRepository extends JpaRepository<WordAttemptLogEntity, Long> {
}
