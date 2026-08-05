package com.iread.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(35)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.qa-demo-dataset.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class QaDemoDatasetInitializer implements ApplicationRunner {

    private final QaDemoDatasetService datasetService;
    private final QaDemoAssetInstaller assetInstaller;

    @Override
    public void run(ApplicationArguments args) {
        if (!datasetService.isPostSeedInstalled()) {
            datasetService.install();
        }
        assetInstaller.installMissing();
    }
}
