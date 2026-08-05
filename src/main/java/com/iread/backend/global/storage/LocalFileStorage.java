package com.iread.backend.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.file-storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

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
        if (file == null) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }
        return store(
                StringUtils.cleanPath(file.getOriginalFilename()),
                file.getContentType(),
                file.getSize(),
                () -> file.getInputStream()
        );
    }

    @Override
    public StoredFile store(String originalFileName, String contentType, byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }
        return store(
                StringUtils.cleanPath(originalFileName),
                contentType,
                content.length,
                () -> new ByteArrayInputStream(content)
        );
    }

    private StoredFile store(
            String originalFileName,
            String contentType,
            long size,
            InputStreamSource source
    ) {
        String extension = validate(originalFileName, contentType, size, source);
        String storeFileName = UUID.randomUUID() + "." + extension;
        Path target = uploadDirectory.resolve(storeFileName).normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 파일 경로입니다.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream inputStream = source.open()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 파일 저장에 실패했습니다.");
        }

        return new StoredFile(
                originalFileName,
                storeFileName,
                size,
                publicUrlPrefix + "/" + storeFileName
        );
    }

    @Override
    public LoadedFile load(String storeFileName) {
        if (storeFileName == null || !storeFileName.matches(
                "[0-9a-f-]{36}\\.(png|jpg|jpeg)"
        )) {
            throw new IllegalArgumentException("올바르지 않은 이미지 파일 이름입니다.");
        }
        Path target = uploadDirectory.resolve(storeFileName).normalize();
        if (!target.getParent().equals(uploadDirectory) || !Files.isRegularFile(target)) {
            throw new com.iread.backend.exception.ResourceNotFoundException(
                    "이미지 파일을 찾을 수 없습니다."
            );
        }
        String contentType = storeFileName.endsWith(".png")
                ? "image/png"
                : "image/jpeg";
        try {
            return new LoadedFile(Files.readAllBytes(target), contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 파일을 읽는 데 실패했습니다.");
        }
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

    private String validate(
            String originalFileName,
            String contentType,
            long size,
            InputStreamSource source
    ) {
        if (size <= 0) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }
        if (size > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드할 수 있습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("JPG 또는 PNG 이미지만 업로드할 수 있습니다.");
        }

        String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("JPG 또는 PNG 이미지만 업로드할 수 있습니다.");
        }
        if (("png".equals(extension) && !"image/png".equals(contentType))
                || (!"png".equals(extension) && !"image/jpeg".equals(contentType))) {
            throw new IllegalArgumentException("이미지 확장자와 형식이 일치하지 않습니다.");
        }
        if (!hasExpectedSignature(source, extension)) {
            throw new IllegalArgumentException("이미지 파일 내용이 JPG 또는 PNG 형식이 아닙니다.");
        }
        return extension;
    }

    private boolean hasExpectedSignature(InputStreamSource source, String extension) {
        try (InputStream inputStream = source.open()) {
            byte[] header = inputStream.readNBytes(8);
            if ("png".equals(extension)) {
                byte[] png = new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                };
                return java.util.Arrays.equals(header, png);
            }
            return header.length >= 3
                    && header[0] == (byte) 0xFF
                    && header[1] == (byte) 0xD8
                    && header[2] == (byte) 0xFF;
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 파일을 확인할 수 없습니다.", exception);
        }
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface InputStreamSource {
        InputStream open() throws IOException;
    }
}
