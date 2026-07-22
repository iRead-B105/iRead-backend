package com.iread.backend.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiClientProperties.class)
public class AiClientConfig {

    @Bean("aiRestClient")
    public RestClient aiRestClient(RestClient.Builder builder, AiClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder aiClientBuilder = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory);

        if (StringUtils.hasText(properties.apiKey())) {
            aiClientBuilder.defaultHeader("X-API-Key", properties.apiKey());
        }

        return aiClientBuilder.build();
    }
}
