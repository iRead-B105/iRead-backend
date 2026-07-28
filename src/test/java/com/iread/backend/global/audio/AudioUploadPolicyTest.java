package com.iread.backend.global.audio;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioUploadPolicyTest {

    private final AudioUploadPolicy policy = new AudioUploadPolicy(
            DataSize.ofMegabytes(20),
            "audio/webm,audio/wav,audio/mpeg,audio/mp4"
    );

    @Test
    void 권장_MIME과_일치하는_확장자를_허용한다() {
        assertThat(policy.validate(audio("voice.webm", "audio/webm")).extension()).isEqualTo("webm");
        assertThat(policy.validate(audio("voice.wav", "audio/wav")).extension()).isEqualTo("wav");
        assertThat(policy.validate(audio("voice.mp3", "audio/mpeg")).extension()).isEqualTo("mp3");
        assertThat(policy.validate(audio("voice.m4a", "audio/mp4")).extension()).isEqualTo("m4a");
    }

    @Test
    void MIME과_확장자가_다르면_거부한다() {
        assertThatThrownBy(() -> policy.validate(audio("voice.mp3", "audio/webm")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("음성 파일 형식과 확장자가 일치하지 않습니다.");
    }

    @Test
    void 허용되지_않은_MIME을_거부한다() {
        assertThatThrownBy(() -> policy.validate(audio("voice.ogg", "audio/ogg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 음성 파일 형식입니다.");
    }

    @Test
    void 원본_파일명의_경로_요소를_제거한다() {
        var validated = policy.validate(audio("../../private/voice.webm", "audio/webm"));

        assertThat(validated.originalFilename()).isEqualTo("voice.webm");
    }

    @Test
    void 이십_메가바이트를_초과하면_거부한다() {
        MultipartFile audio = mock(MultipartFile.class);
        when(audio.isEmpty()).thenReturn(false);
        when(audio.getSize()).thenReturn(DataSize.ofMegabytes(20).toBytes() + 1);

        assertThatThrownBy(() -> policy.validate(audio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("음성 파일은 20MB를 초과할 수 없습니다.");
    }

    private MockMultipartFile audio(String filename, String contentType) {
        return new MockMultipartFile("audioFile", filename, contentType, new byte[]{1});
    }
}
