package com.iread.backend.story.app.service;

import com.iread.backend.global.audio.AudioUploadPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StoryAudioStorageTest {

    @Test
    void storesOriginalAudioUnderStudentStoryDirectory(@TempDir Path tempDir) throws Exception {
        StoryAudioStorage storage = new StoryAudioStorage(tempDir.toString(), audioUploadPolicy());
        var audio = new MockMultipartFile(
                "audioFile", "reading.webm", "audio/webm", new byte[]{1, 2, 3}
        );

        storage.store(20L, audio);

        try (var files = Files.list(tempDir.resolve("20").resolve("story"))) {
            assertThat(files.toList()).singleElement()
                    .satisfies(path -> assertThat(path.getFileName().toString()).endsWith(".webm"));
        }
    }

    @Test
    void storesAndLoadsGeneratedTtsAudio(@TempDir Path tempDir) {
        StoryAudioStorage storage = new StoryAudioStorage(tempDir.toString(), audioUploadPolicy());
        byte[] audio = new byte[]{'I', 'D', '3'};

        String fileName = storage.storeGenerated(20L, audio);

        assertThat(fileName).matches("tts-[0-9a-f-]{36}\\.mp3");
        assertThat(storage.loadGenerated(20L, fileName)).isEqualTo(audio);
    }

    private AudioUploadPolicy audioUploadPolicy() {
        return new AudioUploadPolicy(
                DataSize.ofMegabytes(20),
                "audio/webm,audio/wav,audio/mpeg,audio/mp4"
        );
    }
}
