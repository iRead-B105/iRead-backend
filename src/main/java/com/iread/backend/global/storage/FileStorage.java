package com.iread.backend.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile store(MultipartFile file);
    void delete(String storeFileName);
}
