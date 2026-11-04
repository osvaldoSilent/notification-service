package com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound JSON message payload matching the {@code UserCreatedEvent} published by {@code login-service}
 * to Google Cloud Pub/Sub.
 *
 * @param userId    unique identifier (UUID) of the created user
 * @param email     registered email address of the user
 * @param userName  display username of the user
 * @param role      assigned authorization role
 * @param createdAt timestamp when the user was created
 */
public record UserCreatedMessagePayload(
        UUID userId,
        String email,
        String userName,
        String role,
        Instant createdAt
) {}
