package com.iread.backend.student.service;

import com.iread.backend.gaze.app.service.GazeDataStorage;
import com.iread.backend.global.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Component
public class StudentDeletionResourceCleaner {

    private final FileStorage fileStorage;
    private final GazeDataStorage gazeDataStorage;
    private final String profileImageUrlPrefix;
    private final String gazeDataUrlPrefix;

    public StudentDeletionResourceCleaner(
            FileStorage fileStorage,
            GazeDataStorage gazeDataStorage,
            @Value("${app.file-storage.local.public-url:/uploads/images}") String profileImageUrlPrefix,
            @Value("${app.gaze-storage.local.public-url:/gaze}") String gazeDataUrlPrefix
    ) {
        this.fileStorage = fileStorage;
        this.gazeDataStorage = gazeDataStorage;
        this.profileImageUrlPrefix = normalizePrefix(profileImageUrlPrefix);
        this.gazeDataUrlPrefix = normalizePrefix(gazeDataUrlPrefix);
    }

    public void cleanAfterCommit(
            Long studentId,
            String profileImageUrl,
            List<String> gazeDataUrls
    ) {
        Runnable cleanup = () -> clean(studentId, profileImageUrl, gazeDataUrls);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            cleanup.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cleanup.run();
                    }
                }
        );
    }

    private void clean(Long studentId, String profileImageUrl, List<String> gazeDataUrls) {
        deleteProfileImage(profileImageUrl);
        if (gazeDataUrls == null) {
            return;
        }
        if (gazeDataUrlPrefix.isBlank()) {
            return;
        }
        String studentGazePrefix = gazeDataUrlPrefix + "/" + studentId + "/";
        gazeDataUrls.stream()
                .filter(url -> url != null && url.startsWith(studentGazePrefix))
                .forEach(this::deleteGazeData);
    }

    private void deleteProfileImage(String imageUrl) {
        if (profileImageUrlPrefix.isBlank()
                || imageUrl == null
                || !imageUrl.startsWith(profileImageUrlPrefix + "/")) {
            return;
        }
        int slash = imageUrl.lastIndexOf('/');
        if (slash < 0 || slash == imageUrl.length() - 1) {
            return;
        }
        try {
            fileStorage.delete(imageUrl.substring(slash + 1));
        } catch (RuntimeException exception) {
            log.warn("Failed to delete student profile image: {}", imageUrl, exception);
        }
    }

    private void deleteGazeData(String dataUrl) {
        try {
            gazeDataStorage.delete(dataUrl);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete student gaze data: {}", dataUrl, exception);
        }
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
