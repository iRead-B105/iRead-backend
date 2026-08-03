package com.iread.backend.global.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesJpegAndPngUpToFiveMegabytes() {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        StoredFile stored = storage.store(image);

        assertThat(stored.url()).startsWith("/uploads/images/");
        assertThat(tempDir.resolve(stored.storeFileName())).exists();
    }

    @Test
    void storesValidatedGeneratedImageBytes() {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        StoredFile stored = storage.store("generated.png", "image/png", png);

        assertThat(stored.url()).startsWith("/uploads/images/");
        assertThat(tempDir.resolve(stored.storeFileName())).hasBinaryContent(png);
    }

    @Test
    void rejectsUnsupportedOrMismatchedImageType() {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image",
                "profile.webp",
                "image/webp",
                new byte[]{1}
        ))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image",
                "profile.png",
                "image/jpeg",
                new byte[]{1}
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                bytes
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
