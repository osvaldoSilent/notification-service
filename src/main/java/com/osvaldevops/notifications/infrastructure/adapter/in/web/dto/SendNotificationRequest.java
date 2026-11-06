package com.osvaldevops.notifications.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Request DTO for manually triggering a welcome notification via {@code POST /api/v1/notifications/welcome}.
 */
@Schema(description = "Request payload for dispatching a welcome notification to a user")
public record SendNotificationRequest(
        @Schema(description = "Unique identifier (UUID) of the user", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID userId,

        @Schema(description = "Recipient email address", example = "osval@osvaldevops.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Display username of the recipient", example = "osval", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "User name is required")
        String userName,

        @Schema(description = "Assigned authorization role (defaults to ROLE_USER if omitted)", example = "ROLE_USER", defaultValue = "ROLE_USER", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String role
) {}

