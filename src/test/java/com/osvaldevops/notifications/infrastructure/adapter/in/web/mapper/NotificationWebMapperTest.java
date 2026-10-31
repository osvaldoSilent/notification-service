package com.osvaldevops.notifications.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.SendNotificationRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Unit tests for {@link NotificationWebMapper}.
 */
class NotificationWebMapperTest {

    private final NotificationWebMapper mapper = Mappers.getMapper(NotificationWebMapper.class);

    @Test
    @DisplayName("Should map SendNotificationRequest to UserCreatedEvent")
    void should_MapRequestToEvent_When_RequestIsValid() {
        // Given
        UUID userId = UUID.randomUUID();
        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER"
        );

        // When
        UserCreatedEvent event = mapper.toEvent(request);

        // Then
        assertThat(event).isNotNull();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("osval@osvaldevops.com");
        assertThat(event.userName()).isEqualTo("osval");
        assertThat(event.role()).isEqualTo("ROLE_USER");
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Should map domain WelcomeNotification to NotificationResponse")
    void should_MapDomainToResponse_When_NotificationIsValid() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(id)
                .userId(userId)
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome!")
                .body("Body text")
                .status("SENT")
                .sentAt(now)
                .build();

        // When
        NotificationResponse response = mapper.toResponse(notification);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.recipientEmail()).isEqualTo("osval@osvaldevops.com");
        assertThat(response.recipientName()).isEqualTo("osval");
        assertThat(response.subject()).isEqualTo("Welcome!");
        assertThat(response.body()).isEqualTo("Body text");
        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.sentAt()).isEqualTo(now);
    }
}

