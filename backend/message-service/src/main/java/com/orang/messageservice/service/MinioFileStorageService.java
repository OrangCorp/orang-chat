package com.orang.messageservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioClient externalMinioClient;

    public MinioFileStorageService(
            MinioClient minioClient,
            @Qualifier("externalMinioClient") MinioClient externalMinioClient) {
        this.minioClient = minioClient;
        this.externalMinioClient = externalMinioClient;
    }

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.external-endpoint}")
    private String externalEndpoint;

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
            // Use external client if possible, but handle potential connection issues
            // by falling back to manual signing if it fails, or just ensure it works.
            // Actually, we can just use the internal client but pass the HOST header
            // that we want to be used for signing.

            return externalMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(storageKey)
                            .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL via external client: {}. Falling back to internal + replace.", e.getMessage());
            try {
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(storageKey)
                                .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                                .build()
                );
                String internalEndpointBase = endpoint.replace("http://", "").replace("https://", "");
                String externalEndpointBase = externalEndpoint.replace("http://", "").replace("https://", "");
                return url.replace(internalEndpointBase, externalEndpointBase);
            } catch (Exception ex) {
                log.error("Failed to generate presigned download URL for: {}", storageKey, ex);
                throw new RuntimeException("Failed to generate presigned download URL", ex);
            }
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