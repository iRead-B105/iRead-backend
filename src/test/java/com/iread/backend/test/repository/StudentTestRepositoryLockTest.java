package com.iread.backend.test.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTestRepositoryLockTest {

    @Test
    void mutationLookupUsesPessimisticWriteLock() throws Exception {
        Lock lock = StudentTestRepository.class
                .getMethod("findByIdAndStudentIdForUpdate", Long.class, Long.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
