package com.iread.backend.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.file-storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path uploadDirectory;
    private final String publicUrlPrefix;

    public LocalFileStorage(
            @Value("${app.file-storage.local.upload-dir:uploads/images}") String uploadDirectory,
            @Value("${app.file-storage.local.public-url:/uploads/images}") String publicUrlPrefix
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicUrlPrefix = publicUrlPrefix.replaceAll("/+$", "");
    }

    @Override
    public StoredFile store(MultipartFile file) {
        validate(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        String storeFileName = UUID.randomUUID() + "." + extension;
        Path target = uploadDirectory.resolve(storeFileName).normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 파일 경로입니다.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 파일 저장에 실패했습니다.");
        }

        return new StoredFile(
                originalFileName,
                storeFileName,
                file.getSize(),
                publicUrlPrefix + "/" + storeFileName
        );
    }

    @Override
    public void delete(String storeFileName) {
        if (!StringUtils.hasText(storeFileName)) return;

        Path target = uploadDirectory.resolve(storeFileName).normalize();
        if (!target.getParent().equals(uploadDirectory)) return;

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 파일 삭제에 실패했습니다.");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        String extension = extensionOf(StringUtils.cleanPath(file.getOriginalFilename()));
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
