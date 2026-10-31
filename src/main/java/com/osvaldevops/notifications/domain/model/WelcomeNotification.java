package com.osvaldevops.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Core domain model representing a welcome notification prepared for a newly registered user.
 */
@Getter
@Builder
@AllArgsConstructor
public class WelcomeNotification {

    private final UUID id;
    private final UUID userId;
    private final String recipientEmail;
    private final String recipientName;
    private final String subject;
    private final String body;
    private final String status;
    private final Instant sentAt;
}

