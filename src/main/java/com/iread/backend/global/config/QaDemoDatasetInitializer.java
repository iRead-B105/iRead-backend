package com.iread.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@ConditionalOnProperty(name = "iread.qa-demo-dataset.enabled", havingValue = "true")
public class QaDemoDatasetInitializer implements ApplicationRunner {

    private final QaDemoDatasetService datasetService;
    private final QaDemoAssetInstaller assetInstaller;
    private final String deploymentTag;

    public QaDemoDatasetInitializer(
            QaDemoDatasetService datasetService,
            QaDemoAssetInstaller assetInstaller,
            @Value("${iread.qa-demo-dataset.deploy-tag:}") String deploymentTag
    ) {
        this.datasetService = datasetService;
        this.assetInstaller = assetInstaller;
        this.deploymentTag = deploymentTag;
    }

    @Override
    public void run(ApplicationArguments args) {
        String tag = requireDeploymentTag();
        if (datasetService.isAppliedForDeployment(tag)) {
            return;
        }

        datasetService.install();
        assetInstaller.restore();
        datasetService.recordAppliedDeployment(tag);
    }

    private String requireDeploymentTag() {
        if (deploymentTag == null || deploymentTag.isBlank()) {
            throw new IllegalStateException(
                    "iread.qa-demo-dataset.deploy-tag is required when QA dataset bootstrap is enabled."
            );
        }
        return deploymentTag;
    }
}
