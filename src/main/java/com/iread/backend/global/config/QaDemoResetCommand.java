package com.iread.backend.global.config;

import com.iread.backend.training.config.DemoTrainingProgressResetService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

public final class QaDemoResetCommand {

    private QaDemoResetCommand() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ResetApplication.class)
                .profiles("demo")
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run(args);
        try {
            context.getBean(QaDemoResetService.class).reset();
        } finally {
            context.close();
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({
            QaDemoDatasetService.class,
            QaDemoAssetInstaller.class,
            DemoTrainingProgressResetService.class,
            QaDemoResetService.class
    })
    static class ResetApplication {
    }
}
