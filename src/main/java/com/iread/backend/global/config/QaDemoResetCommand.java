package com.iread.backend.global.config;

import com.iread.backend.IreadBackendApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class QaDemoResetCommand {

    private QaDemoResetCommand() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(IreadBackendApplication.class)
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
}
