package com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Unit tests for {@link UserCreatedEventMapper}.
 */
class UserCreatedEventMapperTest {

    private final UserCreatedEventMapper mapper = Mappers.getMapper(UserCreatedEventMapper.class);

    @Test
    @DisplayName("Should map UserCreatedMessagePayload to domain UserCreatedEvent")
    void should_MapPayloadToDomainEvent_When_PayloadIsValid() {
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

        // When
        UserCreatedEvent event = mapper.toDomainEvent(payload);

        // Then
        assertThat(event).isNotNull();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("osval@osvaldevops.com");
        assertThat(event.userName()).isEqualTo("osval");
        assertThat(event.role()).isEqualTo("ROLE_USER");
        assertThat(event.createdAt()).isEqualTo(now);
    }
}

