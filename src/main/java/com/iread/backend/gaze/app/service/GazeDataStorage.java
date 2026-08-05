package com.iread.backend.gaze.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 원시 시선 데이터를 파일로 저장하고 `gaze_sessions.data_url`에 남길 URL만 돌려준다.
 * 좌표 원본은 DB에 넣지 않으며 URL도 App·교수자 응답에 노출하지 않는다.
 */
@Component
public class GazeDataStorage {

    private static final Pattern STORED_URL = Pattern.compile(
            "/(?<studentId>\\d+)/(?<fileName>gaze-\\d+-[0-9a-f-]{36}\\.json)$"
    );

    private final Path gazeRoot;
    private final String publicUrlPrefix;

    public GazeDataStorage(
            @Value("${app.gaze-storage.local.upload-dir:gaze}") String uploadDirectory,
            @Value("${app.gaze-storage.local.public-url:/gaze}") String publicUrlPrefix
    ) {
        this.gazeRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicUrlPrefix = publicUrlPrefix.replaceAll("/+$", "");
    }

    public String store(Long studentId, Long gazeSessionId, String rawData) {
        if (rawData == null || rawData.isBlank()) {
            throw new IllegalArgumentException("저장할 원시 시선 데이터가 비어 있습니다.");
        }
        String fileName = "gaze-" + gazeSessionId + "-" + UUID.randomUUID() + ".json";
        Path directory = studentDirectory(studentId);
        Path target = directory.resolve(fileName).normalize();
        if (!target.getParent().equals(directory)) {
            throw new IllegalArgumentException("올바르지 않은 시선 데이터 파일 경로입니다.");
        }
        try {
            Files.createDirectories(directory);
            Files.writeString(target, rawData, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("원시 시선 데이터 저장에 실패했습니다.");
        }
        return publicUrlPrefix + "/" + studentId + "/" + fileName;
    }

    public String load(String dataUrl) {
        Path target = resolve(dataUrl);
        if (!Files.isRegularFile(target)) {
            return null;
        }
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("원시 시선 데이터를 읽는 데 실패했습니다.");
        }
    }

    public void overwrite(String dataUrl, String rawData) {
        if (rawData == null || rawData.isBlank()) {
            throw new IllegalArgumentException("저장할 원시 시선 데이터가 비어 있습니다.");
        }
        Path target = resolve(dataUrl);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, rawData, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("원시 시선 데이터 저장에 실패했습니다.");
        }
    }

    public void delete(String dataUrl) {
        Path target = resolve(dataUrl);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("원시 시선 데이터 삭제에 실패했습니다.", exception);
        }
    }

    private Path resolve(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("시선 데이터 URL이 비어 있습니다.");
        }
        var matcher = STORED_URL.matcher(dataUrl);
        if (!matcher.find()) {
            throw new IllegalArgumentException("올바르지 않은 시선 데이터 URL입니다.");
        }
        Path directory = studentDirectory(Long.parseLong(matcher.group("studentId")));
        Path target = directory.resolve(matcher.group("fileName")).normalize();
        if (!target.getParent().equals(directory)) {
            throw new IllegalArgumentException("올바르지 않은 시선 데이터 파일 경로입니다.");
        }
        return target;
    }

    private Path studentDirectory(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("학생 식별자는 필수입니다.");
        }
        Path directory = gazeRoot.resolve(studentId.toString()).normalize();
        if (!directory.startsWith(gazeRoot)) {
            throw new IllegalArgumentException("올바르지 않은 시선 데이터 저장 경로입니다.");
        }
        return directory;
    }
}
