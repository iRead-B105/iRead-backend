package com.iread.backend.global.storage;

public record StoredFile(
        String originalFileName,
        String storeFileName,
        long fileSize,
        String url
) {
}
