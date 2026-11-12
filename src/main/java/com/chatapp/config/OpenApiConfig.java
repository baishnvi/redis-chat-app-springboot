package com.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Redis Chat Application API")
                .version("1.0.0")
                .description("Chat rooms, messaging and real-time updates backed by Redis "
                        + "(Hash for room metadata, Set for participants, List for messages, Pub/Sub for real-time)."));
    }
}
