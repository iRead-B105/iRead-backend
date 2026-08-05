package com.iread.backend.global.config;

import com.iread.backend.training.config.DemoTrainingProgressResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class QaDemoResetService {

    private final QaDemoDatasetService datasetService;
    private final DemoTrainingProgressResetService trainingResetService;
    private final QaDemoAssetInstaller assetInstaller;

    public void reset() {
        datasetService.install();
        DemoTrainingProgressResetService.RESET_CURRICULUM_BY_STUDENT
                .keySet()
                .forEach(trainingResetService::resetIfPresent);
        assetInstaller.restore();
    }
}
