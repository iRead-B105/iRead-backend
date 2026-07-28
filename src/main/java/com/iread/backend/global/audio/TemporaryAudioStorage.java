package com.iread.backend.global.audio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class TemporaryAudioStorage {

    private final Path temporaryDirectory;
    private final AudioUploadPolicy uploadPolicy;

    public TemporaryAudioStorage(
            @Value("${app.audio-upload.temp-dir:${java.io.tmpdir}/iread-audio}") String temporaryDirectory,
            AudioUploadPolicy uploadPolicy
    ) {
        this.temporaryDirectory = Path.of(temporaryDirectory).toAbsolutePath().normalize();
        this.uploadPolicy = uploadPolicy;
    }

    public StagedAudio stage(MultipartFile audioFile) {
        AudioUploadPolicy.ValidatedAudio validated = uploadPolicy.validate(audioFile);
        Path stagedPath = null;
        try {
            Files.createDirectories(temporaryDirectory);
            stagedPath = Files.createTempFile(temporaryDirectory, "audio-", "." + validated.extension());
            try (InputStream inputStream = audioFile.getInputStream()) {
                Files.copy(inputStream, stagedPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StagedAudio(
                    stagedPath,
                    validated.originalFilename(),
                    validated.contentType()
            );
        } catch (IOException exception) {
            deleteQuietly(stagedPath);
            throw new IllegalStateException("임시 음성 파일을 준비하는 데 실패했습니다.", exception);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 원래 예외를 보존한다.
        }
    }

    public static final class StagedAudio implements AutoCloseable {
        private final Path path;
        private final String originalFilename;
        private final String contentType;

        private StagedAudio(Path path, String originalFilename, String contentType) {
            this.path = path;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        public Path path() {
            return path;
        }

        public String originalFilename() {
            return originalFilename;
        }

        public String contentType() {
            return contentType;
        }

        @Override
        public void close() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                throw new IllegalStateException("임시 음성 파일 삭제에 실패했습니다.", exception);
            }
        }
    }
}
