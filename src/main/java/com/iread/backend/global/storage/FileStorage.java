package com.iread.backend.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile store(MultipartFile file);
    StoredFile store(String originalFileName, String contentType, byte[] content);
    void delete(String storeFileName);
}
