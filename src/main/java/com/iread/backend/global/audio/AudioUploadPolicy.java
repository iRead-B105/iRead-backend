package com.iread.backend.global.audio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AudioUploadPolicy {

    private static final Map<String, String> EXTENSION_CONTENT_TYPES = Map.of(
            "webm", "audio/webm",
            "wav", "audio/wav",
            "mp3", "audio/mpeg",
            "m4a", "audio/mp4",
            "mp4", "audio/mp4"
    );

    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public AudioUploadPolicy(
            @Value("${app.audio-upload.max-size:20MB}") DataSize maxSize,
            @Value("${app.audio-upload.allowed-content-types:audio/webm,audio/wav,audio/mpeg,audio/mp4}")
            String allowedContentTypes
    ) {
        this.maxSizeBytes = maxSize.toBytes();
        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
        if (maxSizeBytes <= 0 || this.allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("음성 업로드 정책 설정이 유효하지 않습니다.");
        }
    }

    public ValidatedAudio validate(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("음성 파일이 비어 있습니다.");
        }
        if (audioFile.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("음성 파일은 20MB를 초과할 수 없습니다.");
        }

        String contentType = audioFile.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 음성 파일 형식입니다.");
        }

        String originalFilename = StringUtils.getFilename(
                StringUtils.cleanPath(audioFile.getOriginalFilename())
        );
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("음성 파일 이름이 필요합니다.");
        }
        String extension = extensionOf(originalFilename);
        if (!contentType.equals(EXTENSION_CONTENT_TYPES.get(extension))) {
            throw new IllegalArgumentException("음성 파일 형식과 확장자가 일치하지 않습니다.");
        }
        return new ValidatedAudio(originalFilename, extension, contentType, audioFile.getSize());
    }

    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    public Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("음성 파일 확장자가 필요합니다.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public record ValidatedAudio(
            String originalFilename,
            String extension,
            String contentType,
            long size
    ) {
    }
}
