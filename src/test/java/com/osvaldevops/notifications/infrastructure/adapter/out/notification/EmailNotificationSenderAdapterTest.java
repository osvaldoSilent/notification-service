package com.osvaldevops.notifications.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EmailNotificationSenderAdapter}.
 */
class EmailNotificationSenderAdapterTest {

    private EmailNotificationSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmailNotificationSenderAdapter("no-reply@osvaldevops.com");
    }

    @Test
    @DisplayName("Should dispatch welcome notification without throwing when recipient email is valid")
    void should_SendNotificationSuccessfully_When_NotificationIsValid() {
        // Given
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome!")
                .body("Hello osval!")
                .status("SENT")
                .sentAt(Instant.now())
                .build();

        // When / Then
        assertThatCode(() -> adapter.send(notification)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw NotificationDeliveryException when notification or recipient email is null or blank")
    void should_ThrowNotificationDeliveryException_When_NotificationOrRecipientEmailIsInvalid() {
        // Given
        WelcomeNotification nullEmail = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .recipientEmail(null)
                .build();
        WelcomeNotification blankEmail = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .recipientEmail("   ")
                .build();

        // When / Then
        assertThatThrownBy(() -> adapter.send(null))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");

        assertThatThrownBy(() -> adapter.send(nullEmail))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");

        assertThatThrownBy(() -> adapter.send(blankEmail))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");
    }
}

