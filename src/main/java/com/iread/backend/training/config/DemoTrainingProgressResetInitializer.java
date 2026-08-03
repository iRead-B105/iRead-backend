package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(40)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-training-reset.enabled",
        havingValue = "true"
)
public class DemoTrainingProgressResetInitializer implements ApplicationRunner {

    private final DemoTrainingProgressResetService resetService;

    @Override
    public void run(ApplicationArguments args) {
        DemoTrainingProgressResetService.RESET_CURRICULUM_BY_STUDENT
                .keySet()
                .forEach(resetService::resetIfPresent);
    }
}
