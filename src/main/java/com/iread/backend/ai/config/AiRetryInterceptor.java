package com.iread.backend.ai.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Set;

final class AiRetryInterceptor implements ClientHttpRequestInterceptor {

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(502, 503, 504);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        try {
            ClientHttpResponse first = execution.execute(request, body);
            if (!RETRYABLE_STATUS_CODES.contains(first.getStatusCode().value())) {
                return first;
            }
            first.close();
        } catch (IOException firstFailure) {
            return execution.execute(request, body);
        }
        return execution.execute(request, body);
    }
}
