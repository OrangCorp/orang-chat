package com.orang.messageservice.service;

import com.orang.messageservice.entity.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final FileStorageService fileStorageService;

    @Value("${thumbnail.width:200}")
    private int thumbnailWidth;

    @Value("${thumbnail.height:200}")
    private int thumbnailHeight;

    @Value("${thumbnail.quality:0.8}")
    private double thumbnailQuality;

    public boolean supportsFileType(FileType fileType) {
        return FileType.IMAGE.equals(fileType);
    }

    /**
     * Downloads original image, generates thumbnail, uploads result.
     **/
    public String generateAndUploadThumbnail(
            String originalStorageKey,
            UUID conversationId,
            UUID attachmentId) {

        String thumbnailPath = buildThumbnailPath(conversationId, attachmentId);

        try (InputStream originalStream = fileStorageService.downloadFile(originalStorageKey)) {

            // Generate thumbnail (small size, so in-memory is fine)
            ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream();

            Thumbnails.of(originalStream)
                    .size(thumbnailWidth, thumbnailHeight)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(thumbnailQuality)
                    .toOutputStream(thumbnailOutput);

            byte[] thumbnailBytes = thumbnailOutput.toByteArray();

            // Upload thumbnail to MinIO
            String storageKey = fileStorageService.uploadFile(
                    new ByteArrayInputStream(thumbnailBytes),
                    "thumbnail.jpg",
                    "image/jpeg",
                    thumbnailBytes.length,
                    thumbnailPath
            );

            log.info("Generated thumbnail for attachment {}: {} bytes → {}",
                    attachmentId, thumbnailBytes.length, storageKey);

            return storageKey;

        } catch (IOException e) {
            log.error("Failed to generate thumbnail for attachment {}: {}",
                    attachmentId, e.getMessage(), e);
            return null;
        }
    }

    private String buildThumbnailPath(UUID conversationId, UUID attachmentId) {
        return String.format("%s/%s/thumbnail.jpg", conversationId, attachmentId);
    }
}