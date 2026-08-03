package com.iread.backend.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRetryInterceptorTest {

    private final AiRetryInterceptor interceptor = new AiRetryInterceptor();
    private final HttpRequest request = mock(HttpRequest.class);
    private final byte[] body = "{\"requestId\":\"same-key\"}".getBytes();

    @Test
    void retriesRetryableStatusExactlyOnceWithSameRequest() throws IOException {
        ClientHttpResponse unavailable = response(HttpStatus.SERVICE_UNAVAILABLE);
        ClientHttpResponse success = response(HttpStatus.OK);
        AtomicInteger attempts = new AtomicInteger();

        ClientHttpResponse actual = interceptor.intercept(
                request,
                body,
                (candidateRequest, candidateBody) -> {
                    assertThat(candidateRequest).isSameAs(request);
                    assertThat(candidateBody).isSameAs(body);
                    return attempts.getAndIncrement() == 0 ? unavailable : success;
                }
        );

        assertThat(actual).isSameAs(success);
        assertThat(attempts).hasValue(2);
        verify(unavailable).close();
    }

    @Test
    void doesNotRetryNonRetryableStatus() throws IOException {
        ClientHttpResponse badRequest = response(HttpStatus.BAD_REQUEST);
        AtomicInteger attempts = new AtomicInteger();

        ClientHttpResponse actual = interceptor.intercept(
                request,
                body,
                (candidateRequest, candidateBody) -> {
                    attempts.incrementAndGet();
                    return badRequest;
                }
        );

        assertThat(actual).isSameAs(badRequest);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void retriesConnectionFailureExactlyOnce() throws IOException {
        ClientHttpResponse success = response(HttpStatus.OK);
        AtomicInteger attempts = new AtomicInteger();

        ClientHttpResponse actual = interceptor.intercept(
                request,
                body,
                (candidateRequest, candidateBody) -> {
                    if (attempts.getAndIncrement() == 0) {
                        throw new IOException("connection reset");
                    }
                    return success;
                }
        );

        assertThat(actual).isSameAs(success);
        assertThat(attempts).hasValue(2);
    }

    private ClientHttpResponse response(HttpStatus status) throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(status);
        return response;
    }
}
