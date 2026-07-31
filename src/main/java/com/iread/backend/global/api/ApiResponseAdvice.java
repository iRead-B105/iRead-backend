package com.iread.backend.global.api;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.iread.backend")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        // 오디오처럼 바이너리로 내려보내는 응답은 감싸면 컨버터가 캐스팅에 실패한다.
        if (MediaType.APPLICATION_OCTET_STREAM.includes(selectedContentType)
                || body instanceof byte[]
                || body instanceof Resource) {
            return body;
        }
        if (body instanceof ApiSuccessResponse || body instanceof ApiErrorResponse) {
            return body;
        }
        return ApiSuccessResponse.of(body);
    }
}
