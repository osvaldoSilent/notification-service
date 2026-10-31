package com.osvaldevops.notifications.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a dispatched welcome notification.
 */
@Schema(description = "Response payload representing a dispatched welcome notification")
public record NotificationResponse(
        @Schema(description = "Unique identifier (UUID) of the notification", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Unique identifier (UUID) of the recipient user", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Recipient email address", example = "osval@osvaldevops.com")
        String recipientEmail,

        @Schema(description = "Recipient display name", example = "osval")
        String recipientName,

        @Schema(description = "Notification subject line", example = "Welcome to OsvalDevOps Platform!")
        String subject,

        @Schema(description = "Notification message body", example = "Hello osval, welcome aboard!")
        String body,

        @Schema(description = "Delivery status of the notification", example = "SENT")
        String status,

        @Schema(description = "Timestamp when the notification was sent", example = "2026-10-30T23:55:00Z")
        Instant sentAt
) {}

