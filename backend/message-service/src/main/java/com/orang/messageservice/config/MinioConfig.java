package com.orang.messageservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.external-endpoint}")
    private String externalEndpoint;

    @Bean
    @Primary
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            log.info("MinIO client created: endpoint={}", endpoint);

            ensureBucketExists(client);

            return client;
        } catch (Exception e) {
            log.error("Failed to create MinIO client", e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }

    @Bean
    @Qualifier("externalMinioClient")
    public MinioClient externalMinioClient() {
        try {
            return MinioClient.builder()
                    .endpoint(externalEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            log.error("Failed to create external MinIO client", e);
            throw new RuntimeException("Failed to initialize external MinIO client", e);
        }
    }

    private void ensureBucketExists(MinioClient client) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }
}