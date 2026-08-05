package com.iread.backend.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class QaDemoAssetInstallerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void installsCompressedImagesAndRawGazeFixtures() throws Exception {
        Path images = tempDirectory.resolve("images");
        Path gaze = tempDirectory.resolve("gaze");
        QaDemoAssetInstaller installer = new QaDemoAssetInstaller(images.toString(), gaze.toString());

        installer.restore();

        try (var files = Files.list(images)) {
            assertThat(files).hasSize(12).allMatch(path -> path.toString().endsWith(".jpg"));
        }
        try (var files = Files.walk(gaze)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(3);
        }
        try (var files = Files.list(images)) {
            assertThat(files).allMatch(path -> {
                try {
                    return Files.size(path) <= QaDemoAssetInstaller.MAX_IMAGE_BYTES;
                } catch (Exception exception) {
                    return false;
                }
            });
        }
        assertThat(Files.readString(gaze.resolve(
                "2001/gaze-290101-a0010000-0000-4000-8000-000000000001.json"
        ))).contains("\"storyId\": 280002").contains("\"samples\"");
    }
}
