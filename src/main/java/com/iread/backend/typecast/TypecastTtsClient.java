package com.iread.backend.typecast;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class TypecastTtsClient {
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");

    private final RestClient restClient;
    private final TypecastTtsProperties properties;
    private volatile String resolvedVoiceId;

    public TypecastTtsClient(
            @Qualifier("typecastRestClient") RestClient restClient,
            TypecastTtsProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.resolvedVoiceId = normalized(properties.voiceId());
    }

    public byte[] synthesize(String text, double tempo) {
        requireConfigured();
        try {
            byte[] audio = restClient.post()
                    .uri("/v1/text-to-speech")
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
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw TypecastTtsException.upstream(response.getStatusCode().value());
                    })
                    .requiredBody(byte[].class);
            if (audio.length == 0) {
                throw TypecastTtsException.emptyAudio();
            }
            return audio;
        } catch (TypecastTtsException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw TypecastTtsException.communication(exception);
        }
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
                TypecastVoice[] voices = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v2/voices")
                                .queryParam("model", properties.model())
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (request, response) -> {
                            throw TypecastTtsException.upstream(response.getStatusCode().value());
                        })
                        .requiredBody(TypecastVoice[].class);
                resolvedVoiceId = Arrays.stream(voices)
                        .filter(voice -> voice.voice_name().equalsIgnoreCase(properties.voiceName()))
                        .filter(voice -> voice.models() != null && voice.models().stream()
                                .anyMatch(model -> properties.model().equals(model.version())))
                        .map(TypecastVoice::voice_id)
                        .findFirst()
                        .orElseThrow(() -> TypecastTtsException.voiceNotFound(properties.voiceName()));
                return resolvedVoiceId;
            } catch (TypecastTtsException exception) {
                throw exception;
            } catch (RestClientException exception) {
                throw TypecastTtsException.communication(exception);
            }
        }
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw TypecastTtsException.notConfigured();
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
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
