package com.iread.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "app.file-storage.type", havingValue = "local", matchIfMissing = true)
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDirectory;
    private final String publicUrl;

    public WebConfig(
            @Value("${app.file-storage.local.upload-dir:uploads/images}") String uploadDirectory,
            @Value("${app.file-storage.local.public-url:/uploads/images}") String publicUrl
    ) {
        this.uploadDirectory = uploadDirectory;
        this.publicUrl = publicUrl;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(publicUrl.replaceAll("/+$", "") + "/**")
                .addResourceLocations(location);
    }
}
