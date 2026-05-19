package com.orang.messageservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void messageServiceOpenAPIUsesMessageServiceMetadata() {
        var openApi = new OpenApiConfig().messageServiceOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Message Service API");
        assertThat(openApi.getInfo().getDescription()).isEqualTo("Message and conversation endpoints");
        assertThat(openApi.getServers()).hasSize(2);
    }
}
