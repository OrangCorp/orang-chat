package com.chatapp.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("Web push notification subscription and preferences management")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway"),
                        new Server()
                                .url("http://localhost:8085")
                                .description("Direct (Dev)")
                ));
    }
}