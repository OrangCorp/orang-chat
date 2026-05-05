package com.orang.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("userServiceOpenAPI builds the expected metadata")
    void userServiceOpenApiBuildsExpectedMetadata() {
        OpenAPI openAPI = new OpenApiConfig().userServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("User Service API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("User management endpoints");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().getFirst().getUrl()).isEqualTo("http://localhost:8080");
    }
}