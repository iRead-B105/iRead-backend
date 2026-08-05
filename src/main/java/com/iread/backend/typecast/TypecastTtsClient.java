package com.iread.backend.typecast;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class TypecastTtsClient {
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");
    // Typecast는 크레딧 소진·플랜 제한을 403(빈 본문)으로, 요청 한도를 429로 돌려준다.
    private static final List<Integer> QUOTA_STATUS = List.of(402, 403, 429);
    private static final int INVALID_KEY_STATUS = 401;

    private final RestClient restClient;
    private final TypecastTtsProperties properties;
    private final TypecastKeyRing keyRing;
    private volatile String resolvedVoiceId;

    public TypecastTtsClient(
            @Qualifier("typecastRestClient") RestClient restClient,
            TypecastTtsProperties properties,
            TypecastKeyRing keyRing
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.keyRing = keyRing;
        this.resolvedVoiceId = normalized(properties.voiceId());
    }

    public byte[] synthesize(String text, double tempo) {
        requireConfigured();
        // 할당량 오류로 키가 전환되면 새 키로 즉시 1회 재시도해 이 요청을 살린다.
        try {
            return synthesizeOnce(text, tempo);
        } catch (KeyRotatedException rotated) {
            return synthesizeOnce(text, tempo);
        }
    }

    private byte[] synthesizeOnce(String text, double tempo) {
        try {
            byte[] audio = withKeyHandling(restClient.post()
                    .uri("/v1/text-to-speech")
                    .header("X-API-KEY", keyRing.activeKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(AUDIO_MPEG)
                    .body(Map.of(
                            "voice_id", resolveVoiceId(),
                            "text", text,
                            "model", properties.model(),
                            "language", "kor",
                            "output", Map.of(
                                    "volume", 100,
                                    "audio_pitch", 0,
                                    "audio_tempo", tempo,
                                    "audio_format", "mp3"
                            )
                    ))
                    .retrieve())
                    .requiredBody(byte[].class);
            if (audio.length == 0) {
                throw TypecastTtsException.emptyAudio();
            }
            keyRing.recordSuccess();
            return audio;
        } catch (KeyRotatedException | TypecastTtsException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw TypecastTtsException.communication(exception);
        }
    }

    /**
     * 업스트림 오류를 키 회전 정책에 연결한다. 할당량 오류(402·403·429)가
     * 연속 2회면, 무효 키(401)면 즉시 키를 전환하고 KeyRotatedException으로
     * 재시도를 유도한다. 전환이 없으면 기존 업스트림 예외를 그대로 던진다.
     */
    private ResponseSpec withKeyHandling(ResponseSpec spec) {
        return spec.onStatus(status -> status.value() == INVALID_KEY_STATUS, (request, response) -> {
            if (keyRing.recordInvalidKey()) {
                throw new KeyRotatedException();
            }
            throw TypecastTtsException.upstream(INVALID_KEY_STATUS);
        }).onStatus(status -> QUOTA_STATUS.contains(status.value()), (request, response) -> {
            if (keyRing.recordQuotaFailure()) {
                throw new KeyRotatedException();
            }
            throw TypecastTtsException.upstream(response.getStatusCode().value());
        }).onStatus(status -> status.isError(), (request, response) -> {
            throw TypecastTtsException.upstream(response.getStatusCode().value());
        });
    }

    private String resolveVoiceId() {
        String cached = resolvedVoiceId;
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        synchronized (this) {
            if (StringUtils.hasText(resolvedVoiceId)) {
                return resolvedVoiceId;
            }
            try {
                TypecastVoice[] voices = withKeyHandling(restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v2/voices")
                                .queryParam("model", properties.model())
                                .build())
                        .header("X-API-KEY", keyRing.activeKey())
                        .retrieve())
                        .requiredBody(TypecastVoice[].class);
                resolvedVoiceId = Arrays.stream(voices)
                        .filter(voice -> voice.voice_name().equalsIgnoreCase(properties.voiceName()))
                        .filter(voice -> voice.models() != null && voice.models().stream()
                                .anyMatch(model -> properties.model().equals(model.version())))
                        .map(TypecastVoice::voice_id)
                        .findFirst()
                        .orElseThrow(() -> TypecastTtsException.voiceNotFound(properties.voiceName()));
                return resolvedVoiceId;
            } catch (KeyRotatedException | TypecastTtsException exception) {
                throw exception;
            } catch (RestClientException exception) {
                throw TypecastTtsException.communication(exception);
            }
        }
    }

    private void requireConfigured() {
        if (!keyRing.isConfigured()) {
            throw TypecastTtsException.notConfigured();
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    /** 키가 방금 전환됐음을 알리는 내부 신호. 호출부는 새 키로 1회 재시도한다. */
    static final class KeyRotatedException extends RuntimeException {
        KeyRotatedException() {
            super("Typecast API 키가 전환되었습니다. 재시도합니다.", null, false, false);
        }
    }

    private record TypecastVoice(
            String voice_id,
            String voice_name,
            List<TypecastVoiceModel> models
    ) {
    }

    private record TypecastVoiceModel(String version) {
    }
}
