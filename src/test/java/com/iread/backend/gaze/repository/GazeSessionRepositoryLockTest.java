package com.iread.backend.gaze.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.assertj.core.api.Assertions.assertThat;

class GazeSessionRepositoryLockTest {

    @Test
    void 상태_전이와_분석_저장_조회는_쓰기_잠금을_사용한다() throws Exception {
        Lock lock = GazeSessionRepository.class
                .getMethod("findByIdAndStudentIdForUpdate", Long.class, Long.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
