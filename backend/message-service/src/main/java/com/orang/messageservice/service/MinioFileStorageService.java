package com.orang.messageservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.download-mode:backend}")
    private String downloadMode;

    @Override
    public String uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long fileSize,
            String storagePath) throws IOException {

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Uploaded file to MinIO: bucket={}, path={}, size={}",
                    bucketName, storagePath, fileSize);

            return storagePath;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", storagePath, e);
            throw new IOException("Failed to upload file to storage", e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String storageKey, Duration expiry) {
        if (!"minio".equalsIgnoreCase(downloadMode)) {
            throw new IllegalStateException("Presigned URLs are disabled in backend download mode");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(storageKey)
                            .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL for: {}", storageKey, e);
            throw new RuntimeException("Failed to generate presigned download URL", e);
        }
    }

    @Override
    public InputStream downloadFile(String storageKey) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", storageKey, e);
            throw new IOException("Failed to download file from storage", e);
        }
    }

    @Override
    public void deleteFile(String storageKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
            log.info("Deleted file from MinIO: {}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", storageKey, e);
        }
    }

    @Override
    public boolean fileExists(String storageKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}