package com.orang.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlResponse {
    private String url;
    private int expiresInSeconds;
}