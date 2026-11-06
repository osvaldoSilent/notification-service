package com.osvaldevops.notifications.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration defining API metadata and relative server URL
 * for the Notification Service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the custom {@link OpenAPI} specification for Swagger UI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("1.0.0")
                        .description("Hexagonal Architecture Notification Service on Google Cloud Run consuming GCP Pub/Sub (Push & Pull) events and dispatching SMTP email notifications.")
                        .contact(new Contact()
                                .name("OsvalDevOps")
                                .email("contact@osvaldevops.com")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Default Server (Local / Google Cloud Run)")
                ));
    }
}
