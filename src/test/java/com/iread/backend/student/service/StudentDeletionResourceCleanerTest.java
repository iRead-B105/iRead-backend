package com.iread.backend.student.service;

import com.iread.backend.gaze.app.service.GazeDataStorage;
import com.iread.backend.global.storage.FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudentDeletionResourceCleanerTest {

    @Mock FileStorage fileStorage;
    @Mock GazeDataStorage gazeDataStorage;

    private StudentDeletionResourceCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new StudentDeletionResourceCleaner(
                fileStorage,
                gazeDataStorage,
                "/uploads/images",
                "/gaze"
        );
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void deletesOnlyManagedResourcesForTheDeletedStudent() {
        String ownedGaze = "/gaze/10/gaze-1-owned.json";
        String otherStudentGaze = "/gaze/11/gaze-2-other.json";

        cleaner.cleanAfterCommit(
                10L,
                "/uploads/images/student.png",
                List.of(ownedGaze, otherStudentGaze)
        );

        verify(fileStorage).delete("student.png");
        verify(gazeDataStorage).delete(ownedGaze);
        verify(gazeDataStorage, never()).delete(otherStudentGaze);
    }

    @Test
    void keepsSharedProfileImages() {
        cleaner.cleanAfterCommit(10L, "/images/student-profile.png", List.of());

        verifyNoInteractions(fileStorage, gazeDataStorage);
    }

    @Test
    void waitsUntilTransactionCommitBeforeDeletingFiles() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        cleaner.cleanAfterCommit(
                10L,
                "/uploads/images/student.png",
                List.of("/gaze/10/gaze-1-owned.json")
        );

        verifyNoInteractions(fileStorage, gazeDataStorage);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(fileStorage).delete("student.png");
        verify(gazeDataStorage).delete("/gaze/10/gaze-1-owned.json");
    }

    @Test
    void doesNotDeleteFilesWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        cleaner.cleanAfterCommit(
                10L,
                "/uploads/images/student.png",
                List.of("/gaze/10/gaze-1-owned.json")
        );
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                ));

        verifyNoInteractions(fileStorage, gazeDataStorage);
    }

    @Test
    void continuesCleanupWhenOneGazeFileFails() {
        String first = "/gaze/10/gaze-1-owned.json";
        String second = "/gaze/10/gaze-2-owned.json";
        doThrow(new IllegalStateException("delete failed"))
                .when(gazeDataStorage).delete(first);

        cleaner.cleanAfterCommit(10L, null, List.of(first, second));

        verify(gazeDataStorage).delete(first);
        verify(gazeDataStorage).delete(second);
    }
}
