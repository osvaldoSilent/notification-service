package com.osvaldevops.notifications.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration defining API metadata for the Notifications Service.
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
                        .description("Hexagonal Architecture Notification Service consuming UserCreatedEvent from Apache Kafka and dispatching welcome notifications.")
                        .contact(new Contact()
                                .name("OsvalDevOps")
                                .email("contact@osvaldevops.com")));
    }
}

