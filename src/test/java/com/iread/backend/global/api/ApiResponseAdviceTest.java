package com.iread.backend.global.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseAdviceTest {

    private final ApiResponseAdvice advice = new ApiResponseAdvice();

    private Object write(Object body, MediaType contentType) {
        return advice.beforeBodyWrite(body, null, contentType, null, null, null);
    }

    @Test
    void 일반_응답은_공통_형식으로_감싼다() {
        Object wrapped = write("hello", MediaType.APPLICATION_JSON);

        assertThat(wrapped).isInstanceOf(ApiSuccessResponse.class);
    }

    @Test
    void 오디오_바이트는_감싸지_않는다() {
        // 감싸면 ByteArrayHttpMessageConverter가 byte[]로 캐스팅하다 실패한다.
        byte[] audio = {'I', 'D', '3'};

        Object written = write(audio, MediaType.parseMediaType("audio/mpeg"));

        assertThat(written).isSameAs(audio);
    }

    @Test
    void 이미_공통_형식이면_다시_감싸지_않는다() {
        ApiSuccessResponse already = ApiSuccessResponse.of("hello");

        assertThat(write(already, MediaType.APPLICATION_JSON)).isSameAs(already);
    }
}
