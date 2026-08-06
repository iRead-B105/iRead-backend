package com.iread.backend.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QaDemoDatasetInitializerTest {

    @Test
    void restoresTheQaDatasetForANewDeploymentTag() throws Exception {
        QaDemoDatasetService datasetService = mock(QaDemoDatasetService.class);
        QaDemoAssetInstaller assetInstaller = mock(QaDemoAssetInstaller.class);
        when(datasetService.isAppliedForDeployment("main-abc1234")).thenReturn(false);

        new QaDemoDatasetInitializer(datasetService, assetInstaller, "main-abc1234").run(null);

        verify(datasetService).install();
        verify(assetInstaller).restore();
        verify(datasetService).recordAppliedDeployment("main-abc1234");
    }

    @Test
    void leavesTheQaDatasetUntouchedForTheSameDeploymentTag() throws Exception {
        QaDemoDatasetService datasetService = mock(QaDemoDatasetService.class);
        QaDemoAssetInstaller assetInstaller = mock(QaDemoAssetInstaller.class);
        when(datasetService.isAppliedForDeployment("main-abc1234")).thenReturn(true);

        new QaDemoDatasetInitializer(datasetService, assetInstaller, "main-abc1234").run(null);

        verify(datasetService, never()).install();
        verify(assetInstaller, never()).restore();
        verify(datasetService, never()).recordAppliedDeployment("main-abc1234");
    }

    @Test
    void rejectsAnEnabledBootstrapWithoutADeploymentTag() {
        QaDemoDatasetService datasetService = mock(QaDemoDatasetService.class);
        QaDemoAssetInstaller assetInstaller = mock(QaDemoAssetInstaller.class);

        assertThatIllegalStateException().isThrownBy(
                () -> new QaDemoDatasetInitializer(datasetService, assetInstaller, " ").run(null)
        ).withMessageContaining("deploy-tag");
    }
}
