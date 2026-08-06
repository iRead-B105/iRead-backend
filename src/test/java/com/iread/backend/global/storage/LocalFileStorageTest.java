package com.iread.backend.global.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesJpegAndPngUpToFiveMegabytes() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                imageBytes("jpg")
        );

        StoredFile stored = storage.store(image);

        assertThat(stored.url()).startsWith("/uploads/images/");
        assertThat(tempDir.resolve(stored.storeFileName())).exists();
    }

    @Test
    void storesValidatedGeneratedImageBytes() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        byte[] png = imageBytes("png");

        StoredFile stored = storage.store("generated.png", "image/png", png);

        assertThat(stored.url()).startsWith("/uploads/images/");
        assertThat(tempDir.resolve(stored.storeFileName())).hasBinaryContent(png);
    }

    @Test
    void loadsStoredImageWithItsContentType() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        byte[] png = imageBytes("png");
        StoredFile stored = storage.store("generated.png", "image/png", png);

        LoadedFile loaded = storage.load(stored.storeFileName());

        assertThat(loaded.contentType()).isEqualTo("image/png");
        assertThat(loaded.content()).containsExactly(png);
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

    @Test
    void rejectsNonImageWithAllowedExtensionMimeAndSignature() {
        LocalFileStorage storage = new LocalFileStorage(
                tempDir.toString(),
                "/uploads/images"
        );
        byte[] fakePng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                'n', 'o', 't', '-', 'a', 'n', '-', 'i', 'm', 'a', 'g', 'e'
        };
        byte[] fakeJpeg = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
                'n', 'o', 't', '-', 'a', 'n', '-', 'i', 'm', 'a', 'g', 'e'
        };

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image", "profile.png", "image/png", fakePng
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 내용");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image", "profile.jpg", "image/jpeg", fakeJpeg
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 내용");
    }

    private byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }
}
