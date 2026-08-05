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

        // API 키는 회전(TypecastKeyRing)을 위해 고정 헤더가 아니라
        // 요청 시점에 TypecastTtsClient가 붙인다.
        return builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
