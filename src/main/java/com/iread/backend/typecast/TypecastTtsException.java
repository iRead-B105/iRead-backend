package com.iread.backend.typecast;

import org.springframework.http.HttpStatus;

public class TypecastTtsException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    private TypecastTtsException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public static TypecastTtsException notConfigured() {
        return new TypecastTtsException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TYPECAST_NOT_CONFIGURED",
                "Typecast API 키가 설정되지 않았습니다.",
                null
        );
    }

    public static TypecastTtsException voiceNotFound(String voiceName) {
        return new TypecastTtsException(
                HttpStatus.BAD_GATEWAY,
                "TYPECAST_VOICE_NOT_FOUND",
                "Typecast 음성 " + voiceName + "을 찾을 수 없습니다.",
                null
        );
    }

    public static TypecastTtsException upstream(int upstreamStatus) {
        return new TypecastTtsException(
                HttpStatus.BAD_GATEWAY,
                "TYPECAST_UPSTREAM_ERROR",
                "Typecast 음성 생성 요청에 실패했습니다. (upstream " + upstreamStatus + ")",
                null
        );
    }

    public static TypecastTtsException communication(Throwable cause) {
        return new TypecastTtsException(
                HttpStatus.BAD_GATEWAY,
                "TYPECAST_COMMUNICATION_ERROR",
                "Typecast 서버와 통신하지 못했습니다.",
                cause
        );
    }

    public static TypecastTtsException emptyAudio() {
        return new TypecastTtsException(
                HttpStatus.BAD_GATEWAY,
                "TYPECAST_EMPTY_AUDIO",
                "Typecast가 빈 음성을 반환했습니다.",
                null
        );
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
