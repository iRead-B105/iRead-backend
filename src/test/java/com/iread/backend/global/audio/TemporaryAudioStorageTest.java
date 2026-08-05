package com.iread.backend.global.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryAudioStorageTest {

    @Test
    void 닫히면_임시_음성_파일을_삭제한다(@TempDir Path tempDir) {
        TemporaryAudioStorage storage = new TemporaryAudioStorage(
                tempDir.toString(),
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                )
        );
        var audio = new MockMultipartFile(
                "audioFile", "reading.webm", "audio/webm", new byte[]{1, 2, 3}
        );

        Path stagedPath;
        try (TemporaryAudioStorage.StagedAudio staged = storage.stage(audio)) {
            stagedPath = staged.path();
            assertThat(Files.isRegularFile(stagedPath)).isTrue();
            assertThat(staged.originalFilename()).isEqualTo("reading.webm");
        }

        assertThat(stagedPath).doesNotExist();
    }
}
