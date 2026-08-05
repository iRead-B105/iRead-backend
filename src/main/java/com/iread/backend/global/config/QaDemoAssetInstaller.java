package com.iread.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
@Profile("demo")
public class QaDemoAssetInstaller {

    static final long MAX_IMAGE_BYTES = 500L * 1024L;

    private static final List<String> IMAGE_FILES = List.of(
            "098f386f-8b72-4940-b9b3-d4d197e42dbc.jpg",
            "1b6e8aba-1076-43fb-a9f7-40b4ba68cac6.jpg",
            "2f4abe13-8f84-4d87-b711-06cfe13674c5.jpg",
            "347242ee-73de-4179-bebc-95f4f41d3bdc.jpg",
            "37e5becf-afeb-4472-9b4b-f4ad31804ad7.jpg",
            "5863d881-12c4-44b1-ae36-7cafe2d60108.jpg",
            "5cce6a09-9535-4e1f-8507-52652f2deca9.jpg",
            "77b0b1b1-2794-40d4-903f-54b00f2b03fd.jpg",
            "8d07dd90-efa6-4885-9d54-92f40bd7fa9f.jpg",
            "badf86e5-24c2-4401-920e-51e8f2ce00ac.jpg",
            "dcfbbd01-bc15-4691-bddb-dd9314826709.jpg",
            "fac73006-704f-40b1-abf5-ce4b298d6e33.jpg"
    );

    private static final List<String> GAZE_FILES = List.of(
            "2001/gaze-290101-a0010000-0000-4000-8000-000000000001.json",
            "2002/gaze-290102-a0020000-0000-4000-8000-000000000002.json",
            "2103/gaze-290103-a0030000-0000-4000-8000-000000000003.json"
    );

    private final Path imageDirectory;
    private final Path gazeDirectory;

    public QaDemoAssetInstaller(
            @Value("${app.file-storage.local.upload-dir:uploads/images}") String imageDirectory,
            @Value("${app.gaze-storage.local.upload-dir:gaze}") String gazeDirectory
    ) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
        this.gazeDirectory = Path.of(gazeDirectory).toAbsolutePath().normalize();
    }

    public void installMissing() {
        install(false);
    }

    public void restore() {
        install(true);
    }

    private void install(boolean overwrite) {
        IMAGE_FILES.forEach(fileName -> copy(
                "assets/qa-demo/images/" + fileName,
                imageDirectory.resolve(fileName),
                overwrite,
                MAX_IMAGE_BYTES
        ));
        GAZE_FILES.forEach(relativePath -> copy(
                "assets/qa-demo/gaze/" + relativePath,
                gazeDirectory.resolve(relativePath),
                overwrite,
                Long.MAX_VALUE
        ));
    }

    private void copy(String resourcePath, Path target, boolean overwrite, long maxBytes) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try {
            long size = resource.contentLength();
            if (size > maxBytes) {
                throw new IllegalStateException("QA demo asset exceeds the configured limit: " + resourcePath);
            }
            Files.createDirectories(target.getParent());
            if (!overwrite && Files.exists(target)) {
                return;
            }
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to install QA demo asset: " + resourcePath, exception);
        }
    }
}
