package com.iread.backend.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
            assertThat(files.filter(Files::isRegularFile)).hasSize(57);
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
                .contains("\"sourceTextCoverage\": \"FULL\"")
                .contains("\"schemaVersion\": \"story-gaze-raw-v2\"");
    }

    @Test
    void packagedTrainingGazeFixturesCoverEveryQaChildAndSqlReference() throws Exception {
        var mapper = JsonMapper.builder().build();
        var manifestResource = new ClassPathResource("assets/qa-demo/manifest.json");
        var sqlResource = new ClassPathResource("db/demo-data/qa-demo-reset.sql");
        var manifest = mapper.readTree(manifestResource.getInputStream());
        var sql = new String(sqlResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<Integer, Integer> trainingGazeCounts = new HashMap<>();
        Map<Integer, Integer> testGazeCounts = new HashMap<>();

        assertThat(manifest.path("gaze")).hasSize(57);
        for (var gazeEntry : manifest.path("gaze")) {
            var relativePath = gazeEntry.asText();
            var rawResource = new ClassPathResource("assets/qa-demo/gaze/" + relativePath);
            assertThat(rawResource.exists()).isTrue();
            assertThat(sql).contains("'/gaze/" + relativePath + "'");

            var raw = mapper.readTree(rawResource.getInputStream()).path("rawData");
            var schemaVersion = raw.path("schemaVersion").asText();
            if ("test-gaze-raw-v1".equals(schemaVersion)) {
                var studentId = raw.path("studentId").asInt();
                testGazeCounts.merge(studentId, 1, Integer::sum);
                assertThat(relativePath).startsWith(studentId + "/");
                assertThat(raw.path("testId").asLong()).isPositive();
                assertThat(raw.path("samples")).isNotEmpty();
                continue;
            }
            if (!"training-gaze-raw-v1".equals(schemaVersion)) {
                continue;
            }
            var studentId = raw.path("studentId").asInt();
            trainingGazeCounts.merge(studentId, 1, Integer::sum);
            assertThat(relativePath).startsWith(studentId + "/");
            assertThat(raw.path("synthetic").asBoolean()).isTrue();
            assertThat(raw.path("gazeSessionId").asLong())
                    .isEqualTo(400_000L + raw.path("trainingId").asLong());
            assertThat(raw.path("questions")).hasSize(3);
            assertThat(raw.path("samples")).isNotEmpty();
            assertThat(raw.path("regressions")).isNotEmpty();
        }

        assertThat(trainingGazeCounts).containsExactlyInAnyOrderEntriesOf(
                Map.of(2001, 11, 2002, 15, 2103, 18)
        );
        assertThat(testGazeCounts).containsExactlyInAnyOrderEntriesOf(
                Map.of(2001, 3, 2002, 3, 2103, 3)
        );
    }
}
