package com.osvaldevops.notifications.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a dispatched welcome notification.
 */
@Schema(description = "Response payload representing a dispatched welcome notification")
public record NotificationResponse(
        @Schema(description = "Unique identifier (UUID) of the notification", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Unique identifier (UUID) of the recipient user", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID userId,

        @Schema(description = "Recipient email address", example = "osval@osvaldevops.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String recipientEmail,

        @Schema(description = "Recipient display name", example = "osval", requiredMode = Schema.RequiredMode.REQUIRED)
        String recipientName,

        @Schema(description = "Notification subject line", example = "Welcome to OsvalDevOps Platform!", requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(description = "Notification message body", example = "Hello osval, welcome aboard!", requiredMode = Schema.RequiredMode.REQUIRED)
        String body,

        @Schema(description = "Delivery status of the notification", example = "SENT", requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @Schema(description = "Timestamp when the notification was sent", example = "2026-10-30T23:55:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant sentAt
) {}

