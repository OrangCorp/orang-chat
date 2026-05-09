package com.orang.messageservice.service;

import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioClient externalMinioClient;

    private MinioFileStorageService minioFileStorageService;
    private String bucketName;
    private String storageKey;

    @BeforeEach
    void setUp() throws Exception {
        minioFileStorageService = new MinioFileStorageService(minioClient, externalMinioClient);
        bucketName = "test-bucket";
        storageKey = "conversation/12345/file.jpg";

        // Set configuration via reflection
        ReflectionTestUtils.setField(minioFileStorageService, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioFileStorageService, "externalEndpoint", "http://external:9000");
        ReflectionTestUtils.setField(minioFileStorageService, "bucketName", bucketName);
        ReflectionTestUtils.setField(minioFileStorageService, "downloadMode", "backend");
    }

    // ============ uploadFile Tests ============

    @Test
    void uploadFileSuccessfullyUploadsToMinio() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test.txt";
        String contentType = "text/plain";
        long fileSize = 12L;
        String storagePath = "conversation/123/test.txt";

        when(minioClient.putObject(any())).thenReturn(null);

        String result = minioFileStorageService.uploadFile(
                inputStream, fileName, contentType, fileSize, storagePath);

        assertNotNull(result);
        assertEquals(storagePath, result);
        verify(minioClient).putObject(any());
    }

    @Test
    void uploadFileThrowsWhenMinioFails() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test.txt";
        String contentType = "text/plain";
        long fileSize = 12L;
        String storagePath = "conversation/123/test.txt";

        doThrow(new RuntimeException("MinIO connection failed"))
                .when(minioClient).putObject(any());

        assertThrows(IOException.class,
                () -> minioFileStorageService.uploadFile(
                        inputStream, fileName, contentType, fileSize, storagePath));

        verify(minioClient).putObject(any());
    }

    // ============ generatePresignedDownloadUrl Tests ============

    @Test
    void generatePresignedDownloadUrlReturnsUrlForMinioMode() throws Exception {
        // Set download mode to "minio" so presigned URLs are enabled
        ReflectionTestUtils.setField(minioFileStorageService, "downloadMode", "minio");

        String presignedUrl = "http://localhost:9000/test-bucket/file.jpg?token=abc123";

        when(externalMinioClient.getPresignedObjectUrl(any()))
                .thenReturn(presignedUrl);

        String result = minioFileStorageService.generatePresignedDownloadUrl(storageKey, Duration.ofHours(1));

        assertEquals(presignedUrl, result);
        verify(externalMinioClient).getPresignedObjectUrl(any());
    }

    @Test
    void generatePresignedDownloadUrlHandlesDefaultDuration() throws Exception {
        // Set download mode to "minio" so presigned URLs are enabled
        ReflectionTestUtils.setField(minioFileStorageService, "downloadMode", "minio");

        String presignedUrl = "http://localhost:9000/test-bucket/file.jpg?token=abc123";

        when(externalMinioClient.getPresignedObjectUrl(any()))
                .thenReturn(presignedUrl);

        String result = minioFileStorageService.generatePresignedDownloadUrl(storageKey, Duration.ofHours(1));

        assertEquals(presignedUrl, result);
        verify(externalMinioClient).getPresignedObjectUrl(any());
    }

    @Test
    void generatePresignedDownloadUrlThrowsWhenMinioFails() throws Exception {
        // Set download mode to "minio" so presigned URLs are enabled
        ReflectionTestUtils.setField(minioFileStorageService, "downloadMode", "minio");

        doThrow(new RuntimeException("MinIO connection error"))
                .when(externalMinioClient).getPresignedObjectUrl(any());
        doThrow(new RuntimeException("MinIO connection error"))
                .when(minioClient).getPresignedObjectUrl(any());

        assertThrows(RuntimeException.class,
                () -> minioFileStorageService.generatePresignedDownloadUrl(storageKey, Duration.ofHours(1)));
    }

    // ============ downloadFile Tests ============

    @Test
    void downloadFileReturnsInputStream() throws Exception {
        // Create a mock GetObjectResponse
        io.minio.GetObjectResponse mockResponse = mock(io.minio.GetObjectResponse.class);
        InputStream mockInputStream = new ByteArrayInputStream("file content".getBytes());
        when(mockResponse.read()).thenReturn((int) 'a');

        when(minioClient.getObject(any()))
                .thenReturn(mockResponse);

        InputStream result = minioFileStorageService.downloadFile(storageKey);

        assertNotNull(result);
        verify(minioClient).getObject(any());
    }

    @Test
    void downloadFileThrowsWhenMinioFails() throws Exception {
        doThrow(new RuntimeException("MinIO download failed"))
                .when(minioClient).getObject(any());

        assertThrows(IOException.class,
                () -> minioFileStorageService.downloadFile(storageKey));
    }

    // ============ deleteFile Tests ============

    @Test
    void deleteFileSuccessfullyDeletesFromMinio() throws Exception {
        minioFileStorageService.deleteFile(storageKey);

        verify(minioClient).removeObject(any());
    }

    @Test
    void deleteFileHandlesMinioErrors() throws Exception {
        doThrow(new RuntimeException("MinIO delete failed"))
                .when(minioClient).removeObject(any());

        // deleteFile silently handles errors, so this should not throw
        minioFileStorageService.deleteFile(storageKey);

        verify(minioClient).removeObject(any());
    }

    // ============ fileExists Tests ============

    @Test
    void fileExistsReturnsTrueWhenFilePresent() throws Exception {
        StatObjectResponse statResponse = mock(StatObjectResponse.class);
        when(minioClient.statObject(any()))
                .thenReturn(statResponse);

        boolean result = minioFileStorageService.fileExists(storageKey);

        assertTrue(result);
        verify(minioClient).statObject(any());
    }

    @Test
    void fileExistsReturnsFalseWhenFileNotFound() throws Exception {
        when(minioClient.statObject(any()))
                .thenThrow(new RuntimeException("File not found"));

        boolean result = minioFileStorageService.fileExists(storageKey);

        assertFalse(result);
    }

    @Test
    void fileExistsReturnsFalseWhenMinioErrors() throws Exception {
        when(minioClient.statObject(any()))
                .thenThrow(new RuntimeException("MinIO error"));

        boolean result = minioFileStorageService.fileExists(storageKey);

        assertFalse(result);
    }
}
