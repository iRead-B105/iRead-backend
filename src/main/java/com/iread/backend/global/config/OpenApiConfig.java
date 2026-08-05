package com.iread.backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iReadOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("iRead API")
                        .description("iRead 관리자 앱 및 훈련 앱 API 문서")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("app")
                .pathsToMatch("/api/app/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }
}
