package com.iread.backend.typecast;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TypecastTtsProperties.class)
public class TypecastTtsConfig {

    @Bean
    @Qualifier("typecastRestClient")
    public RestClient typecastRestClient(
            RestClient.Builder builder,
            TypecastTtsProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder typecastBuilder = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory);
        if (StringUtils.hasText(properties.apiKey())) {
            typecastBuilder.defaultHeader("X-API-KEY", properties.apiKey());
        }
        return typecastBuilder.build();
    }
}
