package com.orang.messageservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public interface FileStorageService {

    String uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long fileSize,
            String storagePath
    ) throws IOException;

    String generatePresignedDownloadUrl(String storageKey, Duration duration);
    InputStream downloadFile(String storageKey) throws IOException;
    void deleteFile(String storageKey);
    boolean fileExists(String storageKey);
}