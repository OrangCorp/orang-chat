package com.orang.messageservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI messageServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Message Service API")
                        .description("Message and conversation endpoints")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway"),
                        new Server()
                                .url("http://localhost:8084")
                                .description("Direct (Dev)")
                ));
    }
}