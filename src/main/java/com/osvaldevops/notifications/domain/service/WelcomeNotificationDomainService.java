package com.osvaldevops.notifications.domain.service;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.domain.port.out.NotificationSenderPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Pure domain service implementing {@link SendWelcomeNotificationUseCase} to validate
 * incoming {@link UserCreatedEvent} payloads, compose welcome messages, and dispatch them
 * via {@link NotificationSenderPort}.
 */
@RequiredArgsConstructor
public class WelcomeNotificationDomainService implements SendWelcomeNotificationUseCase {

    private static final String DEFAULT_SUBJECT = "Welcome to OsvalDevOps Platform!";
    private static final String STATUS_SENT = "SENT";

    private final NotificationSenderPort notificationSenderPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public WelcomeNotification sendWelcomeNotification(UserCreatedEvent event) {
        // Validate that the event and recipient email are present
        if (event == null) {
            throw new IllegalArgumentException("UserCreatedEvent must not be null");
        }
        if (event.email() == null || event.email().isBlank()) {
            throw new IllegalArgumentException("Recipient email must not be null or blank");
        }

        // Resolve display name, falling back to the email address if userName is omitted
        String displayName = (event.userName() == null || event.userName().isBlank())
                ? event.email()
                : event.userName();

        // Compose welcome notification message body
        String messageBody = String.format(
                "Hello %s, welcome aboard! Your account (%s) with role %s has been successfully registered.",
                displayName,
                event.email(),
                event.role() != null ? event.role() : "ROLE_USER"
        );

        WelcomeNotification notification = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .userId(event.userId())
                .recipientEmail(event.email())
                .recipientName(displayName)
                .subject(DEFAULT_SUBJECT)
                .body(messageBody)
                .status(STATUS_SENT)
                .sentAt(Instant.now())
                .build();

        // Dispatch notification through outbound port
        notificationSenderPort.send(notification);

        return notification;
    }
}

