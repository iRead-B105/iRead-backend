package com.iread.backend.global.storage;

public record LoadedFile(
        byte[] content,
        String contentType
) {
}
