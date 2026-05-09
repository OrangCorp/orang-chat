package com.orang.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("notificationServiceOpenAPI exposes the expected metadata")
    void notificationServiceOpenApiExposesExpectedMetadata() {
        OpenAPI openAPI = new OpenApiConfig().notificationServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Notification Service API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("Web push notification subscription and preferences management");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getServers()).hasSize(2);
    }
}