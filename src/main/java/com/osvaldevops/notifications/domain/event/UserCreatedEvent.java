package com.osvaldevops.notifications.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a user registration event consumed from Google Cloud Pub/Sub.
 *
 * @param userId    unique identifier (UUID) of the newly created user
 * @param email     registered email address of the user
 * @param userName  display username of the user
 * @param role      assigned authorization role
 * @param createdAt timestamp when the user account was created
 */
public record UserCreatedEvent(
        UUID userId,
        String email,
        String userName,
        String role,
        Instant createdAt
) {}
