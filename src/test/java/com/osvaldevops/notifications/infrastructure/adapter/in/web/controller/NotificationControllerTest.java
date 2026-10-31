package com.osvaldevops.notifications.infrastructure.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.SendNotificationRequest;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.mapper.NotificationWebMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link NotificationController}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;

    @Mock
    private NotificationWebMapper notificationWebMapper;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    @DisplayName("Should return 201 CREATED with NotificationResponse when welcome notification is sent")
    void should_ReturnCreatedWithNotificationResponse_When_SendWelcomeSucceeds() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Instant now = Instant.now();

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER"
        );
        UserCreatedEvent event = new UserCreatedEvent(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(notificationId)
                .userId(userId)
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome to OsvalDevOps Platform!")
                .body("Hello osval, welcome aboard!")
                .status("SENT")
                .sentAt(now)
                .build();
        NotificationResponse expectedResponse = new NotificationResponse(
                notificationId,
                userId,
                "osval@osvaldevops.com",
                "osval",
                "Welcome to OsvalDevOps Platform!",
                "Hello osval, welcome aboard!",
                "SENT",
                now
        );

        when(notificationWebMapper.toEvent(request)).thenReturn(event);
        when(sendWelcomeNotificationUseCase.sendWelcomeNotification(event)).thenReturn(notification);
        when(notificationWebMapper.toResponse(notification)).thenReturn(expectedResponse);

        // When
        ResponseEntity<NotificationResponse> response = notificationController.sendWelcome(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(notificationWebMapper).toEvent(request);
        verify(sendWelcomeNotificationUseCase).sendWelcomeNotification(event);
        verify(notificationWebMapper).toResponse(notification);
    }
}

