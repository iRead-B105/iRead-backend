package com.iread.backend.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class QaDemoAssetInstallerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void installsCompressedImagesAndRawGazeFixtures() throws Exception {
        Path images = tempDirectory.resolve("images");
        Path gaze = tempDirectory.resolve("gaze");
        Files.createDirectories(images);
        Files.createDirectories(gaze.resolve("2001"));
        Path staleImage = images.resolve("1b6e8aba-1076-43fb-a9f7-40b4ba68cac6.jpg");
        Path staleGeneratedImage = images.resolve("qa-demo-story-280004-scene-03.jpg");
        Path staleGaze = gaze.resolve(
                "2001/gaze-290101-a0010000-0000-4000-8000-000000000001.json"
        );
        Path unrelatedImage = images.resolve("user-upload.jpg");
        Files.writeString(staleImage, "old demo image");
        Files.writeString(staleGeneratedImage, "old generated demo image");
        Files.writeString(staleGaze, "old demo gaze");
        Files.writeString(unrelatedImage, "preserve user data");
        QaDemoAssetInstaller installer = new QaDemoAssetInstaller(
                images.toString(),
                gaze.toString(),
                JsonMapper.builder().build()
        );

        installer.restore();

        assertThat(staleImage).doesNotExist();
        assertThat(staleGeneratedImage).doesNotExist();
        assertThat(staleGaze).doesNotExist();
        assertThat(unrelatedImage).exists();
        try (var files = Files.list(images)) {
            assertThat(files).hasSize(40).allMatch(path -> path.toString().endsWith(".jpg"));
        }
        try (var files = Files.list(images)) {
            assertThat(files.filter(path -> !path.equals(unrelatedImage))).allMatch(path ->
                    path.getFileName().toString().matches(
                            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.jpg"
                    )
            );
        }
        try (var files = Files.walk(gaze)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(13);
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
        Path completedStoryGaze;
        try (var files = Files.list(gaze.resolve("2103"))) {
            completedStoryGaze = files
                    .filter(path -> path.getFileName().toString().startsWith("gaze-290103-"))
                    .findFirst()
                    .orElseThrow();
        }
        assertThat(Files.readString(completedStoryGaze))
                .contains("\"storyId\": 280003")
                .contains("\"tokenCoverage\": \"FULL\"");
    }
}
