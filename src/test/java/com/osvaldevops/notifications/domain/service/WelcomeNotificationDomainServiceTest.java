package com.osvaldevops.notifications.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.out.NotificationSenderPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WelcomeNotificationDomainService}.
 */
@ExtendWith(MockitoExtension.class)
class WelcomeNotificationDomainServiceTest {

    @Mock
    private NotificationSenderPort notificationSenderPort;

    @InjectMocks
    private WelcomeNotificationDomainService welcomeNotificationDomainService;

    @Test
    @DisplayName("Should compose and dispatch welcome notification when UserCreatedEvent is valid")
    void should_SendWelcomeNotification_When_EventIsValid() {
        // Given
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                Instant.now()
        );

        // When
        WelcomeNotification result = welcomeNotificationDomainService.sendWelcomeNotification(event);

        // Then
        ArgumentCaptor<WelcomeNotification> captor = ArgumentCaptor.forClass(WelcomeNotification.class);
        verify(notificationSenderPort).send(captor.capture());

        WelcomeNotification sent = captor.getValue();
        assertThat(result).isEqualTo(sent);
        assertThat(sent.getId()).isNotNull();
        assertThat(sent.getUserId()).isEqualTo(userId);
        assertThat(sent.getRecipientEmail()).isEqualTo("osval@osvaldevops.com");
        assertThat(sent.getRecipientName()).isEqualTo("osval");
        assertThat(sent.getSubject()).isEqualTo("Welcome to OsvalDevOps Platform!");
        assertThat(sent.getBody()).contains("Hello osval", "osval@osvaldevops.com", "ROLE_USER");
        assertThat(sent.getStatus()).isEqualTo("SENT");
        assertThat(sent.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fallback to email as display name and ROLE_USER when userName and role are null or blank")
    void should_FallbackToDefaults_When_UserNameIsBlankAndRoleIsNull() {
        // Given
        UserCreatedEvent eventWithBlankName = new UserCreatedEvent(
                UUID.randomUUID(),
                "fallback@osvaldevops.com",
                "   ",
                null,
                Instant.now()
        );
        UserCreatedEvent eventWithNullName = new UserCreatedEvent(
                UUID.randomUUID(),
                "nullname@osvaldevops.com",
                null,
                "ROLE_ADMIN",
                Instant.now()
        );

        // When
        WelcomeNotification firstResult = welcomeNotificationDomainService.sendWelcomeNotification(eventWithBlankName);
        WelcomeNotification secondResult = welcomeNotificationDomainService.sendWelcomeNotification(eventWithNullName);

        // Then
        assertThat(firstResult.getRecipientName()).isEqualTo("fallback@osvaldevops.com");
        assertThat(firstResult.getBody()).contains("ROLE_USER");
        assertThat(secondResult.getRecipientName()).isEqualTo("nullname@osvaldevops.com");
        assertThat(secondResult.getBody()).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when UserCreatedEvent is null")
    void should_ThrowIllegalArgumentException_When_EventIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> welcomeNotificationDomainService.sendWelcomeNotification(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UserCreatedEvent must not be null");

        verify(notificationSenderPort, never()).send(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when recipient email is null or blank")
    void should_ThrowIllegalArgumentException_When_EmailIsNullOrBlank() {
        // Given
        UserCreatedEvent nullEmailEvent = new UserCreatedEvent(UUID.randomUUID(), null, "osval", "ROLE_USER", Instant.now());
        UserCreatedEvent blankEmailEvent = new UserCreatedEvent(UUID.randomUUID(), "  ", "osval", "ROLE_USER", Instant.now());

        // When / Then
        assertThatThrownBy(() -> welcomeNotificationDomainService.sendWelcomeNotification(nullEmailEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recipient email must not be null or blank");

        assertThatThrownBy(() -> welcomeNotificationDomainService.sendWelcomeNotification(blankEmailEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recipient email must not be null or blank");

        verify(notificationSenderPort, never()).send(any());
    }
}

