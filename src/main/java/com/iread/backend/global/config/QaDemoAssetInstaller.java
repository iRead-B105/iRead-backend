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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("demo")
public class QaDemoAssetInstaller {

    static final long MAX_IMAGE_BYTES = 1024L * 1024L;
    private static final String MANIFEST_PATH = "assets/qa-demo/manifest.json";

    private final Path imageDirectory;
    private final Path gazeDirectory;
    private final ObjectMapper objectMapper;

    public QaDemoAssetInstaller(
            @Value("${app.file-storage.local.upload-dir:uploads/images}") String imageDirectory,
            @Value("${app.gaze-storage.local.upload-dir:gaze}") String gazeDirectory,
            ObjectMapper objectMapper
    ) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
        this.gazeDirectory = Path.of(gazeDirectory).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public void installMissing() {
        install(false);
    }

    public void restore() {
        install(true);
    }

    private void install(boolean overwrite) {
        JsonNode manifest = readManifest();
        if (overwrite) {
            entries(manifest, "staleImages").forEach(fileName -> deleteManaged(
                    imageDirectory,
                    fileName
            ));
            entries(manifest, "staleGaze").forEach(relativePath -> deleteManaged(
                    gazeDirectory,
                    relativePath
            ));
        }
        entries(manifest, "images").forEach(fileName -> copy(
                "assets/qa-demo/images/" + fileName,
                managedPath(imageDirectory, fileName),
                overwrite,
                MAX_IMAGE_BYTES
        ));
        entries(manifest, "gaze").forEach(relativePath -> copy(
                "assets/qa-demo/gaze/" + relativePath,
                managedPath(gazeDirectory, relativePath),
                overwrite,
                Long.MAX_VALUE
        ));
        entries(manifest, "pronunciation").forEach(relativePath -> validateResource(
                "assets/qa-demo/pronunciation/" + relativePath
        ));
    }

    private JsonNode readManifest() {
        ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
        try (InputStream input = resource.getInputStream()) {
            JsonNode manifest = objectMapper.readTree(input);
            if (manifest == null || !manifest.isObject()) {
                throw new IllegalStateException("QA demo asset manifest must be a JSON object.");
            }
            return manifest;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read QA demo asset manifest.", exception);
        }
    }

    private List<String> entries(JsonNode manifest, String fieldName) {
        JsonNode entries = manifest.path(fieldName);
        if (!entries.isArray()) {
            throw new IllegalStateException("QA demo manifest field must be an array: " + fieldName);
        }
        return java.util.stream.StreamSupport.stream(entries.spliterator(), false)
                .map(JsonNode::asText)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private void validateResource(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing QA demo asset: " + resourcePath);
        }
    }

    private Path managedPath(Path root, String relativePath) {
        Path target = root.resolve(relativePath).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("QA demo asset path escapes its storage root: " + relativePath);
        }
        return target;
    }

    private void deleteManaged(Path root, String relativePath) {
        try {
            Files.deleteIfExists(managedPath(root, relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to remove stale QA demo asset: " + relativePath, exception);
        }
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
