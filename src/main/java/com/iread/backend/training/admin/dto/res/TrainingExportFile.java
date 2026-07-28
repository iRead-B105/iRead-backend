package com.iread.backend.training.admin.dto.res;

public record TrainingExportFile(
        String fileName,
        byte[] content
) {
}
