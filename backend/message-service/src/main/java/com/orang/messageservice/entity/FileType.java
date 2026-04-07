package com.orang.messageservice.entity;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum FileType {

    IMAGE(Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    )),

    VIDEO(Set.of(
            "video/mp4",
            "video/quicktime",
            "video/webm"
    )),

    DOCUMENT(Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.oasis.opendocument.text"
    )),

    AUDIO(Set.of(
            "audio/mpeg",
            "audio/mp4"
    ));

    private final Set<String> allowedMimeTypes;

    FileType(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public boolean supports(String mimeType) {
        if (mimeType == null) return false;
        return allowedMimeTypes.contains(mimeType.toLowerCase());
    }

    public static Optional<FileType> fromMimeType(String mimeType) {
        if (mimeType == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(type -> type.supports(mimeType))
                .findFirst();
    }

    public static boolean isSupported(String mimeType) {
        return fromMimeType(mimeType).isPresent();
    }
}
