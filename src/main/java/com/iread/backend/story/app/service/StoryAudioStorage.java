package com.iread.backend.story.app.service;

import com.iread.backend.global.audio.AudioUploadPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class StoryAudioStorage {

    private final Path audioRoot;
    private final AudioUploadPolicy uploadPolicy;

    public StoryAudioStorage(
            @Value("${app.audio-storage.local.upload-dir:audio}") String uploadDirectory,
            AudioUploadPolicy uploadPolicy
    ) {
        this.audioRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.uploadPolicy = uploadPolicy;
    }

    public void store(Long studentId, MultipartFile audioFile) {
        AudioUploadPolicy.ValidatedAudio validated = uploadPolicy.validate(audioFile);
        Path studentStoryDirectory = audioRoot.resolve(studentId.toString()).resolve("story").normalize();
        if (!studentStoryDirectory.startsWith(audioRoot)) {
            throw new IllegalArgumentException("올바르지 않은 음성 저장 경로입니다.");
        }

        Path target = studentStoryDirectory.resolve(UUID.randomUUID() + "." + validated.extension()).normalize();
        if (!target.getParent().equals(studentStoryDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 음성 파일 경로입니다.");
        }

        try {
            Files.createDirectories(studentStoryDirectory);
            try (InputStream inputStream = audioFile.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("음성 파일 저장에 실패했습니다.");
        }
    }

    public String storeGenerated(Long studentId, byte[] audio) {
        if (audio == null || audio.length == 0) {
            throw new IllegalArgumentException("생성된 음성 파일이 비어 있습니다.");
        }
        Path generatedDirectory = audioRoot.resolve(studentId.toString())
                .resolve("story")
                .resolve("tts")
                .normalize();
        String fileName = "tts-" + UUID.randomUUID() + ".mp3";
        Path target = generatedDirectory.resolve(fileName).normalize();
        try {
            Files.createDirectories(generatedDirectory);
            Files.write(target, audio);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("생성된 음성 파일 저장에 실패했습니다.");
        }
    }

    public byte[] loadGenerated(Long studentId, String fileName) {
        if (fileName == null || !fileName.matches("tts-[0-9a-f-]{36}\\.mp3")) {
            throw new IllegalArgumentException("올바르지 않은 음성 파일 이름입니다.");
        }
        Path generatedDirectory = audioRoot.resolve(studentId.toString())
                .resolve("story")
                .resolve("tts")
                .normalize();
        Path target = generatedDirectory.resolve(fileName).normalize();
        if (!target.getParent().equals(generatedDirectory) || !Files.isRegularFile(target)) {
            throw new com.iread.backend.exception.ResourceNotFoundException(
                    "음성 파일을 찾을 수 없습니다."
            );
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw new IllegalStateException("음성 파일을 읽는 데 실패했습니다.");
        }
    }

}
