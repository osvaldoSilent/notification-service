package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link KafkaUserCreatedConsumerAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class KafkaUserCreatedConsumerAdapterTest {

    @Mock
    private SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;

    @Mock
    private UserCreatedEventMapper userCreatedEventMapper;

    @InjectMocks
    private KafkaUserCreatedConsumerAdapter consumerAdapter;

    @Test
    @DisplayName("Should map Kafka payload to UserCreatedEvent and invoke SendWelcomeNotificationUseCase")
    void should_InvokeSendWelcomeNotification_When_KafkaMessageReceived() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        UserCreatedMessagePayload payload = new UserCreatedMessagePayload(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );
        UserCreatedEvent domainEvent = new UserCreatedEvent(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );

        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);

        // When
        consumerAdapter.onUserCreated(payload);

        // Then
        verify(userCreatedEventMapper).toDomainEvent(payload);
        verify(sendWelcomeNotificationUseCase).sendWelcomeNotification(domainEvent);
    }
}

